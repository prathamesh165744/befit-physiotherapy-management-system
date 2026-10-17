package com.befit.backend.service;

import com.befit.backend.dto.*;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Invoices, payments, edits and the audit trail.
 *
 * Rules enforced here (not only in the UI):
 *  - every call checks the invoice's own branch_id against the caller (BranchAccess)
 *  - totals are always recomputed by BillCalculator, never taken from the client
 *  - invoices are row-locked while edited/paid, so concurrent requests cannot corrupt the balance
 *  - a payment can carry an idempotency key, so a double click / retry cannot record it twice
 *  - PAID invoices are locked; only an ADMIN may apply a controlled correction (reason required, fully audited)
 *  - CANCELLED invoices are never edited; nothing is ever deleted
 */
@Service
public class BillingService {
    private static final Set<String> SORTABLE = Set.of("billDate", "totalAmount", "id", "patientName", "status", "amountPaid");

    private final BillRepository billRepository;
    private final BillItemRepository itemRepository;
    private final PaymentRepository paymentRepository;
    private final BillAuditLogRepository auditRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final BranchService branchService;
    private final PatientService patientService;
    private final BranchAccess access;

    @Value("${befit.billing.report-max-rows:5000}") private int reportMaxRows;
    @Value("${befit.billing.export-max-rows:20000}") private int exportMaxRows;

    public BillingService(BillRepository billRepository, BillItemRepository itemRepository, PaymentRepository paymentRepository,
                          BillAuditLogRepository auditRepository, AppointmentRepository appointmentRepository,
                          PatientRepository patientRepository, BranchService branchService, PatientService patientService,
                          BranchAccess access) {
        this.billRepository = billRepository;
        this.itemRepository = itemRepository;
        this.paymentRepository = paymentRepository;
        this.auditRepository = auditRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.branchService = branchService;
        this.patientService = patientService;
        this.access = access;
    }

    // =================================================================== reading

    /** The filter shared by the list, Print All and Excel export. */
    public record Filter(String q, BillStatus status, LocalDate from, LocalDate to) {}

    public record ExportFile(byte[] bytes, String fileName) {}

    @Transactional(readOnly = true)
    public PageResponse<BillDTO> list(AuthUser user, String branch, Filter f, int page, int size, String sort, String dir) {
        List<String> ids = access.visibleBranchIds(user, branch);
        Params p = params(f);
        String prop = SORTABLE.contains(sort) ? sort : "billDate";
        Sort.Direction d = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort s = "id".equals(prop) ? Sort.by(d, "id") : Sort.by(d, prop).and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), s);
        Page<Bill> result = billRepository.search(ids, p.from, p.to, p.statuses, p.q, p.pid, pageable);
        Map<String, String> names = branchService.nameMap();
        return PageResponse.of(result, b -> BillDTO.of(b, names.get(b.getBranchId())));
    }

    /** Everything that matches the filters, ignoring pagination, for the "Print All" report. */
    @Transactional(readOnly = true)
    public BillReportDTO report(AuthUser user, String branch, Filter f) {
        List<String> ids = access.visibleBranchIds(user, branch);
        Map<String, String> names = branchService.nameMap();
        List<Bill> found = fetchAll(ids, f, reportMaxRows + 1);
        boolean truncated = found.size() > reportMaxRows;
        List<BillDTO> rows = (truncated ? found.subList(0, reportMaxRows) : found).stream()
                .map(b -> BillDTO.of(b, names.get(b.getBranchId()))).toList();
        return new BillReportDTO(branchLabel(ids, names), filterSummary(f), LocalDateTime.now(), user.fullName(),
                rows, totals(rows), truncated, reportMaxRows);
    }

    /** Same rows as the report, as an .xlsx file. Refuses (instead of silently cutting rows) when over the cap. */
    @Transactional(readOnly = true)
    public ExportFile export(AuthUser user, String branch, Filter f) {
        List<String> ids = access.visibleBranchIds(user, branch);
        Map<String, String> names = branchService.nameMap();
        List<Bill> found = fetchAll(ids, f, exportMaxRows + 1);
        if (found.size() > exportMaxRows) {
            throw new ApiException(422, "EXPORT_TOO_LARGE",
                    "Too many records to export at once (limit " + exportMaxRows + "). Please narrow the date range or filters.");
        }
        List<BillDTO> rows = found.stream().map(b -> BillDTO.of(b, names.get(b.getBranchId()))).toList();
        try {
            byte[] bytes = BillExcelExporter.export(rows, branchLabel(ids, names), filterSummary(f), user.fullName());
            String scope = ids.size() == 1 ? ids.get(0).toLowerCase() : "all-branches";
            return new ExportFile(bytes, "befit-billing-" + scope + "-" + LocalDate.now() + ".xlsx");
        } catch (IOException e) {
            throw new ApiException(500, "EXPORT_FAILED", "Could not create the Excel file.");
        }
    }

    @Transactional(readOnly = true)
    public BillDetailDTO detail(AuthUser user, Long id) {
        Bill b = billRepository.findById(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        access.requireBranch(user, b.getBranchId());
        return buildDetail(user, b);
    }

    @Transactional(readOnly = true)
    public List<BillAuditDTO> audit(AuthUser user, Long id) {
        Bill b = billRepository.findById(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        access.requireBranch(user, b.getBranchId());
        return auditRepository.findByBillIdOrderByChangedAtDescIdDesc(id).stream().map(BillAuditDTO::of).toList();
    }

    // =================================================================== creating

    /** POST /api/billing/manual - walk-in / product sale / any invoice not tied to an appointment. */
    @Transactional
    public BillDetailDTO createManual(AuthUser user, BillRequests.Create r) {
        String branchId = access.branchForCreate(user, r.branchId());
        Long patientId = null;
        String patientName = r.patientName() == null ? null : r.patientName().trim();
        if (r.patientId() != null) {
            Patient p = patientService.load(r.patientId());
            patientService.requireStaffAccess(user, p);
            patientId = p.getId();
            patientName = p.getFullName();
        }
        if (patientName == null || patientName.isBlank()) throw ApiException.badRequest("Customer / patient name is required.");

        List<BillCalculator.Line> lines = linesOf(r);
        Bill b = issueBill(branchId, patientId, patientName, null, r.description(), lines,
                r.discountAmount(), r.taxPercent(), LocalDateTime.now(), user.email());
        if (r.payNow() == null || r.payNow()) {
            recordPayment(b, b.getTotalAmount(), r.paymentMethod() == null ? PaymentMethod.CASH : r.paymentMethod(),
                    r.referenceNo(), r.idempotencyKey(), user.email(), LocalDateTime.now());
        }
        return buildDetail(user, b);
    }

    /** POST /api/billing/appointment/{id} - bills a visit and marks it COMPLETED. The invoice keeps the APPOINTMENT's branch. */
    @Transactional
    public BillDetailDTO createForAppointment(AuthUser user, Long appointmentId, BillRequests.Create r) {
        Appointment apt = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> ApiException.notFound("Appointment not found."));
        access.requireBranch(user, apt.getBranchId());
        if (billRepository.existsByAppointmentIdAndStatusNot(appointmentId, BillStatus.CANCELLED)) {
            throw ApiException.conflict("ALREADY_BILLED", "This appointment has already been billed.");
        }
        if (apt.getStatus() != AppointmentStatus.CHECKED_IN && apt.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw ApiException.conflict("NOT_BILLABLE", "Only checked-in appointments can be billed.");
        }
        Patient patient = apt.getPatient();
        String desc = r.description() != null && !r.description().isBlank() ? r.description()
                : (apt.getCaseType() == null ? "Consultation" : apt.getCaseType()) + " Charges";
        BillRequests.Create withDesc = new BillRequests.Create(null, null, desc, r.amount(), r.items(), r.discountAmount(),
                r.taxPercent(), r.paymentMethod(), r.payNow(), r.referenceNo(), r.idempotencyKey(), null);

        Bill b = issueBill(apt.getBranchId(), patient == null ? null : patient.getId(),
                patient == null || patient.getFullName() == null ? "Walk-in Patient" : patient.getFullName(),
                apt.getId(), desc, linesOf(withDesc), r.discountAmount(), r.taxPercent(), LocalDateTime.now(), user.email());

        apt.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(apt);

        if (r.payNow() == null || r.payNow()) {
            recordPayment(b, b.getTotalAmount(), r.paymentMethod() == null ? PaymentMethod.CASH : r.paymentMethod(),
                    r.referenceNo(), r.idempotencyKey(), user.email(), LocalDateTime.now());
        }
        return buildDetail(user, b);
    }

    /**
     * Core "create an invoice" step. Also used by the demo-data seeder (that is why it takes a date and has no user checks).
     * The caller is responsible for authorisation.
     */
    @Transactional
    public Bill issueBill(String branchId, Long patientId, String patientName, Long appointmentId, String description,
                          List<BillCalculator.Line> lines, BigDecimal discount, BigDecimal taxPercent,
                          LocalDateTime billDate, String actor) {
        BillCalculator.Result calc = BillCalculator.calculate(lines, discount, taxPercent);
        Bill b = new Bill();
        b.setBranchId(branchId);
        b.setPatientId(patientId);
        b.setPatientName(patientName);
        b.setAppointmentId(appointmentId);
        b.setDescription(description != null && !description.isBlank() ? description.trim() : lines.get(0).description());
        b.setBillDate(billDate);
        b.setCreatedBy(actor);
        b.setUpdatedBy(actor);
        b.setUpdatedAt(LocalDateTime.now());
        b.setRevision(0);
        b.setStatus(BillStatus.UNPAID);
        b.setAmountPaid(BigDecimal.ZERO.setScale(2));
        applyCalc(b, calc);
        b = billRepository.save(b);
        b.setInvoiceNumber(invoiceNumber(branchId, b.getId()));
        List<BillItem> items = saveItems(b, lines, calc);
        audit(b, "CREATED", actor, null, null, snapshot(b, items));
        return b;
    }

    // =================================================================== editing

    @Transactional
    public BillDetailDTO update(AuthUser user, Long id, BillRequests.Update r) {
        Bill b = billRepository.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        access.requireBranch(user, b.getBranchId());
        String lock = lockReason(user, b);
        if (lock != null) throw ApiException.conflict("INVOICE_LOCKED", lock);
        int current = b.getRevision() == null ? 0 : b.getRevision();
        if (r.expectedRevision() != null && r.expectedRevision() != current) {
            throw ApiException.conflict("INVOICE_CHANGED", "This invoice was changed by someone else. Please reload it and try again.");
        }

        List<BillCalculator.Line> lines = r.items().stream()
                .map(i -> new BillCalculator.Line(i.description(), i.quantity() == null ? 0 : i.quantity(), i.unitPrice())).toList();
        BillCalculator.Result calc = BillCalculator.calculate(lines, r.discountAmount(), r.taxPercent());

        BigDecimal paid = BillCalculator.money(b.getAmountPaid());
        if (calc.total().compareTo(paid) < 0) {
            throw ApiException.badRequest("The new total (" + calc.total().toPlainString() + ") is lower than the amount already paid ("
                    + paid.toPlainString() + "). Payments are never removed - cancel the invoice and issue a new one instead.");
        }

        Map<String, Object> before = snapshot(b, itemRepository.findByBillIdOrderByPositionAsc(id));

        itemRepository.deleteByBillId(id);
        applyCalc(b, calc);
        if (r.description() != null && !r.description().isBlank()) b.setDescription(r.description().trim());
        b.setStatus(BillCalculator.statusFor(calc.total(), paid, false));
        b.setRevision(current + 1);
        b.setUpdatedBy(user.email());
        b.setUpdatedAt(LocalDateTime.now());
        List<BillItem> items = saveItems(b, lines, calc);
        billRepository.save(b);

        audit(b, b.getStatus() == BillStatus.PAID && before.get("status").equals("PAID") ? "CORRECTED" : "EDITED",
                user.email(), r.reason().trim(), before, snapshot(b, items));
        return buildDetail(user, b);
    }

    @Transactional
    public BillDetailDTO addPayment(AuthUser user, Long id, BillRequests.Payment r) {
        Bill b = billRepository.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        access.requireBranch(user, b.getBranchId());
        recordPayment(b, r.amount(), r.method(), r.referenceNo(), r.idempotencyKey(), user.email(), LocalDateTime.now());
        return buildDetail(user, b);
    }

    @Transactional
    public BillDetailDTO cancel(AuthUser user, Long id, String reason) {
        if (!user.isAdmin()) throw ApiException.forbidden("Only an administrator can cancel an invoice.");
        Bill b = billRepository.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        if (b.getStatus() == BillStatus.CANCELLED) throw ApiException.conflict("ALREADY_CANCELLED", "This invoice is already cancelled.");
        Map<String, Object> before = snapshot(b, itemRepository.findByBillIdOrderByPositionAsc(id));
        b.setStatus(BillStatus.CANCELLED);
        b.setRevision((b.getRevision() == null ? 0 : b.getRevision()) + 1);
        b.setUpdatedBy(user.email());
        b.setUpdatedAt(LocalDateTime.now());
        billRepository.save(b);
        // Payments stay exactly as they are (history is never rewritten). Any refund is handled outside the system.
        audit(b, "CANCELLED", user.email(), reason.trim(), before, snapshot(b, itemRepository.findByBillIdOrderByPositionAsc(id)));
        return buildDetail(user, b);
    }

    /**
     * Records money received. Caller must already hold the row lock on the invoice (or own a brand-new one).
     * Safe to retry: the same idempotencyKey returns the existing payment instead of adding another one.
     */
    @Transactional
    public Payment recordPayment(Bill b, BigDecimal amount, PaymentMethod method, String referenceNo, String idempotencyKey,
                                 String actor, LocalDateTime when) {
        String key = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
        if (key != null) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                if (!existing.get().getBill().getId().equals(b.getId())) {
                    throw ApiException.conflict("DUPLICATE_KEY", "This payment reference was already used for another invoice.");
                }
                return existing.get(); // same request sent twice -> same payment, nothing added
            }
        }
        if (b.getStatus() == BillStatus.CANCELLED) throw ApiException.conflict("INVOICE_CANCELLED", "Cannot take a payment on a cancelled invoice.");
        BigDecimal amt = BillCalculator.money(amount);
        if (amt.signum() <= 0) throw ApiException.badRequest("Payment amount must be greater than zero.");
        BigDecimal balance = BillCalculator.money(b.getTotalAmount()).subtract(BillCalculator.money(b.getAmountPaid()));
        if (balance.signum() <= 0) throw ApiException.conflict("ALREADY_PAID", "This invoice is already fully paid.");
        if (amt.compareTo(balance) > 0) {
            throw ApiException.badRequest("Payment (" + amt.toPlainString() + ") is more than the outstanding balance (" + balance.toPlainString() + ").");
        }

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("amountPaid", BillCalculator.money(b.getAmountPaid()));
        before.put("status", b.getStatus() == null ? null : b.getStatus().name());

        Payment pay = new Payment();
        pay.setBill(b);
        pay.setBranchId(b.getBranchId());
        pay.setAmount(amt);
        pay.setMethod(method == null ? PaymentMethod.CASH : method);
        pay.setStatus(PaymentStatus.COMPLETED);
        pay.setPaymentDate(when);
        pay.setReferenceNo(referenceNo == null || referenceNo.isBlank() ? null : referenceNo.trim());
        pay.setIdempotencyKey(key);
        pay.setReceivedBy(actor);
        pay = paymentRepository.save(pay);

        BigDecimal newPaid = BillCalculator.money(b.getAmountPaid()).add(amt);
        b.setAmountPaid(newPaid);
        b.setStatus(BillCalculator.statusFor(b.getTotalAmount(), newPaid, false));
        b.setPaymentMethod(summariseMethods(b.getId(), pay));
        b.setUpdatedBy(actor);
        b.setUpdatedAt(when);
        billRepository.save(b);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("amountPaid", newPaid);
        after.put("status", b.getStatus().name());
        after.put("paymentAmount", amt);
        after.put("method", pay.getMethod().name());
        after.put("referenceNo", pay.getReferenceNo());
        audit(b, "PAYMENT_ADDED", actor, null, before, after);
        return pay;
    }

    // =================================================================== helpers

    private record Params(String q, Long pid, LocalDateTime from, LocalDateTime to, List<BillStatus> statuses) {}

    /** Turns an optional filter into values that are never null (PostgreSQL cannot type a null query parameter). */
    private Params params(Filter f) {
        String raw = f == null || f.q() == null ? "" : f.q().trim();
        String q = raw.isEmpty() ? "%" : "%" + raw.toLowerCase() + "%";
        long pid = -1L;
        String digits = raw.toLowerCase().startsWith("reg-") ? raw.substring(4) : raw;
        if (!digits.isEmpty() && digits.length() <= 15 && digits.chars().allMatch(Character::isDigit)) pid = Long.parseLong(digits);
        LocalDateTime from = f == null || f.from() == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : f.from().atStartOfDay();
        LocalDateTime to = f == null || f.to() == null ? LocalDateTime.of(2999, 12, 31, 0, 0) : f.to().plusDays(1).atStartOfDay();
        List<BillStatus> statuses = f == null || f.status() == null ? List.of(BillStatus.values()) : List.of(f.status());
        return new Params(q, pid, from, to, statuses);
    }

    private List<Bill> fetchAll(List<String> ids, Filter f, int limit) {
        Params p = params(f);
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("billDate"), Sort.Order.desc("id")));
        return billRepository.searchAll(ids, p.from, p.to, p.statuses, p.q, p.pid, pageable);
    }

    private BillReportDTO.Totals totals(List<BillDTO> rows) {
        BigDecimal sub = BigDecimal.ZERO, disc = BigDecimal.ZERO, tax = BigDecimal.ZERO, total = BigDecimal.ZERO,
                paid = BigDecimal.ZERO, out = BigDecimal.ZERO;
        for (BillDTO r : rows) {
            if ("CANCELLED".equals(r.status())) continue; // listed, but not counted in the money totals
            sub = sub.add(r.subtotal()); disc = disc.add(r.discountAmount()); tax = tax.add(r.taxAmount());
            total = total.add(r.totalAmount()); paid = paid.add(r.amountPaid()); out = out.add(r.balanceDue());
        }
        return new BillReportDTO.Totals(rows.size(), sub, disc, tax, total, paid, out);
    }

    private String branchLabel(List<String> ids, Map<String, String> names) {
        if (ids.size() == 1) return names.getOrDefault(ids.get(0), ids.get(0));
        return "All branches";
    }

    private String filterSummary(Filter f) {
        if (f == null) return "None";
        List<String> parts = new ArrayList<>();
        if (f.q() != null && !f.q().isBlank()) parts.add("Search: \"" + f.q().trim() + "\"");
        if (f.status() != null) parts.add("Status: " + f.status());
        if (f.from() != null) parts.add("From: " + f.from());
        if (f.to() != null) parts.add("To: " + f.to());
        return parts.isEmpty() ? "None (all invoices)" : String.join(" | ", parts);
    }

    private List<BillCalculator.Line> linesOf(BillRequests.Create r) {
        if (r.items() != null && !r.items().isEmpty()) {
            return r.items().stream()
                    .map(i -> new BillCalculator.Line(i.description(), i.quantity() == null ? 0 : i.quantity(), i.unitPrice())).toList();
        }
        // legacy single-amount request: { amount, description }
        if (r.amount() == null) throw ApiException.badRequest("Please add at least one line item or an amount.");
        String d = r.description() == null || r.description().isBlank() ? "Service charges" : r.description().trim();
        return List.of(new BillCalculator.Line(d, 1, r.amount()));
    }

    private void applyCalc(Bill b, BillCalculator.Result c) {
        b.setSubtotal(c.subtotal());
        b.setDiscountAmount(c.discount());
        b.setTaxPercent(c.taxPercent());
        b.setTaxAmount(c.tax());
        b.setTotalAmount(c.total());
        b.setAmount(c.total().doubleValue()); // legacy mirror
    }

    private List<BillItem> saveItems(Bill b, List<BillCalculator.Line> lines, BillCalculator.Result calc) {
        List<BillItem> items = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            BillCalculator.Line l = lines.get(i);
            items.add(new BillItem(b, l.description().trim(), l.quantity(), BillCalculator.money(l.unitPrice()), calc.lineTotals().get(i), i));
        }
        return itemRepository.saveAll(items);
    }

    static String invoiceNumber(String branchId, Long id) {
        return "INV-" + branchId + "-" + String.format("%06d", id);
    }

    /** One method name if all payments used the same one, MIXED if they differ. */
    private String summariseMethods(Long billId, Payment justSaved) {
        Set<String> methods = new LinkedHashSet<>();
        for (Payment p : paymentRepository.findByBillIdOrderByPaymentDateAscIdAsc(billId)) {
            if (p.getStatus() == PaymentStatus.COMPLETED) methods.add(p.getMethod().name());
        }
        methods.add(justSaved.getMethod().name());
        return methods.size() == 1 ? methods.iterator().next() : "MIXED";
    }

    /** Why this user cannot change the invoice's amounts right now (null = they can). */
    private String lockReason(AuthUser user, Bill b) {
        if (b.getStatus() == BillStatus.CANCELLED) return "A cancelled invoice cannot be edited.";
        if (b.getStatus() == BillStatus.PAID && !user.isAdmin()) {
            return "This invoice is fully paid and locked. Ask an administrator to apply a correction.";
        }
        return null;
    }

    private Map<String, Object> snapshot(Bill b, List<BillItem> items) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("invoiceNumber", b.getInvoiceNumber());
        m.put("status", b.getStatus() == null ? null : b.getStatus().name());
        m.put("description", b.getDescription());
        List<Map<String, Object>> lines = new ArrayList<>();
        for (BillItem i : items) {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("description", i.getDescription());
            l.put("quantity", i.getQuantity());
            l.put("unitPrice", i.getUnitPrice());
            l.put("lineTotal", i.getLineTotal());
            lines.add(l);
        }
        m.put("items", lines);
        m.put("subtotal", b.getSubtotal());
        m.put("discountAmount", b.getDiscountAmount());
        m.put("taxPercent", b.getTaxPercent());
        m.put("taxAmount", b.getTaxAmount());
        m.put("totalAmount", b.getTotalAmount());
        m.put("amountPaid", b.getAmountPaid());
        return m;
    }

    private void audit(Bill b, String action, String actor, String reason, Map<String, Object> before, Map<String, Object> after) {
        auditRepository.save(new BillAuditLog(b.getId(), b.getBranchId(), action, actor, reason,
                before == null ? null : JsonUtil.toJson(before), after == null ? null : JsonUtil.toJson(after)));
    }

    private BillDetailDTO buildDetail(AuthUser user, Bill b) {
        Map<String, String> names = branchService.nameMap();
        BillDTO dto = BillDTO.of(b, names.get(b.getBranchId()));

        List<BillDetailDTO.Item> items = new ArrayList<>();
        for (BillItem i : itemRepository.findByBillIdOrderByPositionAsc(b.getId())) {
            items.add(new BillDetailDTO.Item(i.getId(), i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal()));
        }
        if (items.isEmpty()) { // old invoice not yet converted: show it as one line
            items.add(new BillDetailDTO.Item(null, dto.description() == null ? "Service charges" : dto.description(), 1, dto.subtotal(), dto.subtotal()));
        }
        List<BillDetailDTO.Pay> pays = new ArrayList<>();
        for (Payment p : paymentRepository.findByBillIdOrderByPaymentDateAscIdAsc(b.getId())) {
            pays.add(new BillDetailDTO.Pay(p.getId(), p.getAmount(), p.getMethod().name(), p.getStatus().name(), p.getPaymentDate(),
                    p.getReferenceNo(), p.getReceivedBy()));
        }

        BranchDTO branch = b.getBranchId() == null ? null : BranchDTO.of(branchService.require(b.getBranchId()));
        BillDetailDTO.PatientInfo patient = null;
        if (b.getPatientId() != null) {
            Patient p = patientRepository.findById(b.getPatientId()).orElse(null);
            if (p != null) patient = new BillDetailDTO.PatientInfo(p.getId(), "REG-" + p.getId(), p.getFullName(), p.getPhone(), p.getGender(), p.getAddress());
        }
        if (patient == null) patient = new BillDetailDTO.PatientInfo(null, null, b.getPatientName(), null, null, null);

        BillDetailDTO.AppointmentInfo appt = null;
        if (b.getAppointmentId() != null) {
            Appointment a = appointmentRepository.findById(b.getAppointmentId()).orElse(null);
            if (a != null) appt = new BillDetailDTO.AppointmentInfo(a.getId(), a.getAppointmentDate(), a.getDoctorName(), a.getCaseType());
        }

        String lock = lockReason(user, b);
        boolean cancelled = b.getStatus() == BillStatus.CANCELLED;
        boolean canPay = !cancelled && dto.balanceDue().signum() > 0;
        return new BillDetailDTO(dto, items, pays, branch, patient, appt, lock == null, canPay, user.isAdmin() && !cancelled, lock);
    }
}
