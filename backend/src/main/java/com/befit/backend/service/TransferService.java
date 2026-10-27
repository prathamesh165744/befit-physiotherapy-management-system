package com.befit.backend.service;

import com.befit.backend.dto.StaffTransferRequest;
import com.befit.backend.dto.TransferDTO;
import com.befit.backend.dto.TransferRequestBody;
import com.befit.backend.dto.TransferValidationDTO;
import com.befit.backend.dto.TransferValidationDTO.Issue;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Permanent patient branch transfers.
 *
 * What a transfer changes:  patients.branch_id + a new patient_branch_history row.
 * What a transfer NEVER touches: appointments, bills, payments. They keep the branch_id they were created with,
 *                                so the old branch keeps its history and can still collect its own invoices.
 *
 * WHO may do what
 *   create (staff)  : ADMIN, or a RECEPTIONIST of the patient's CURRENT (source) branch or of the DESTINATION branch.
 *   create (patient): the patient themself.
 *   approve/reject  : ADMIN, or a RECEPTIONIST of the DESTINATION branch. Not the person who raised the request
 *                     (befit.transfer.allow-self-approval=false, the default). Doctors never.
 *   cancel          : the patient (own request), ADMIN, or a RECEPTIONIST of the source or destination branch.
 *
 * CLINIC RULES checked when a request is approved (shown to the approver by validate()):
 *   BLOCKERS  - the patient's branch changed since the request; the destination is inactive; the patient has an open
 *               appointment due today or overdue (complete or cancel it first).
 *   WARNINGS  - upcoming appointments (they stay booked at their original branch); invoices with money still due (they stay
 *               at their original branch, which keeps collecting them). The approver must tick "acknowledge" and write a
 *               note; the acknowledged warnings are stored on the request.
 *               befit.transfer.block-on-outstanding-invoices=true turns the invoice warning into a blocker.
 *
 * befit.transfer.approval-required=true (default): patient/staff asks -> destination receptionist (or admin) approves.
 * befit.transfer.approval-required=false          : a PATIENT's own request is applied immediately (old behaviour); blockers still apply.
 * Either way a branch_transfer_requests row is written, so every move is auditable.
 */
@Service
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final EnumSet<AppointmentStatus> OPEN = EnumSet.of(AppointmentStatus.SCHEDULED, AppointmentStatus.WAITING,
            AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_PROGRESS);
    private static final EnumSet<BillStatus> DUE = EnumSet.of(BillStatus.UNPAID, BillStatus.PARTIAL);

    private final PatientRepository patientRepository;
    private final PatientBranchHistoryRepository historyRepository;
    private final BranchTransferRequestRepository transferRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final BranchService branchService;
    private final BranchAccess access;
    private final boolean approvalRequired;
    private final boolean blockOnOutstandingInvoices;
    private final boolean allowSelfApproval;

    public TransferService(PatientRepository patientRepository, PatientBranchHistoryRepository historyRepository,
                           BranchTransferRequestRepository transferRepository, AppointmentRepository appointmentRepository,
                           BillRepository billRepository, BranchService branchService, BranchAccess access,
                           @Value("${befit.transfer.approval-required:true}") boolean approvalRequired,
                           @Value("${befit.transfer.block-on-outstanding-invoices:false}") boolean blockOnOutstandingInvoices,
                           @Value("${befit.transfer.allow-self-approval:false}") boolean allowSelfApproval) {
        this.patientRepository = patientRepository;
        this.historyRepository = historyRepository;
        this.transferRepository = transferRepository;
        this.appointmentRepository = appointmentRepository;
        this.billRepository = billRepository;
        this.branchService = branchService;
        this.access = access;
        this.approvalRequired = approvalRequired;
        this.blockOnOutstandingInvoices = blockOnOutstandingInvoices;
        this.allowSelfApproval = allowSelfApproval;
    }

    // ------------------------------------------------------------------ patient side

    @Transactional
    public TransferDTO requestTransfer(AuthUser user, TransferRequestBody body) {
        Patient p = patientOf(user);
        return startTransfer(p, body.branchId(), body.reason(), user, false, false);
    }

    @Transactional(readOnly = true)
    public List<TransferDTO> myTransfers(AuthUser user) {
        Patient p = patientOf(user);
        return transferRepository.findByPatientIdOrderByRequestedAtDesc(p.getId(), PageRequest.of(0, 10)).stream()
                .map(t -> toDto(t, p.getFullName(), branchService.nameMap())).toList();
    }

    @Transactional
    public TransferDTO cancelMine(AuthUser user) {
        Patient p = patientOf(user);
        BranchTransferRequest t = transferRepository.findFirstByPatientIdAndStatus(p.getId(), TransferStatus.PENDING)
                .orElseThrow(() -> ApiException.notFound("You have no pending transfer request."));
        close(t, TransferStatus.CANCELLED, user, "Cancelled by the patient", null);
        return toDto(transferRepository.save(t), p.getFullName(), branchService.nameMap());
    }

    // ------------------------------------------------------------------ staff side

    /**
     * Staff creates a transfer request. Always PENDING: someone else (the destination branch, or an admin) decides.
     * Only structure is checked here (branches valid and different, no duplicate pending request); the appointment /
     * invoice rules are evaluated and shown to the approver at decision time, when they are actually true.
     */
    @Transactional
    public TransferDTO createByStaff(AuthUser user, StaffTransferRequest r) {
        requireStaffRole(user);
        Patient p = patientRepository.findById(r.patientId()).orElseThrow(() -> ApiException.notFound("Patient not found."));
        boolean allowed = user.isAdmin()
                || (user.branchId() != null && (user.branchId().equals(p.getBranchId()) || user.branchId().equals(r.toBranchId())));
        if (!allowed) throw ApiException.forbidden("You can only request transfers for patients of your branch, or into your branch.");
        if (r.fromBranchId() != null && !r.fromBranchId().isBlank() && !r.fromBranchId().equals(p.getBranchId())) {
            throw ApiException.conflict("TRANSFER_STALE", "The patient is no longer registered at the branch you selected. Please refresh.");
        }
        return startTransfer(p, r.toBranchId(), r.reason(), user, false, true);
    }

    /** direction: INCOMING (requests into the caller's branch, default) or OUTGOING (requests leaving it). */
    @Transactional(readOnly = true)
    public List<TransferDTO> listForStaff(AuthUser user, String branch, String status, String direction) {
        requireStaffRole(user);
        List<String> ids = access.visibleBranchIds(user, branch);
        boolean outgoing = "OUTGOING".equalsIgnoreCase(direction);
        PageRequest page = PageRequest.of(0, 100);
        List<BranchTransferRequest> rows;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            rows = outgoing ? transferRepository.findByFromBranchIdInOrderByRequestedAtDesc(ids, page)
                            : transferRepository.findByToBranchIdInOrderByRequestedAtDesc(ids, page);
        } else {
            TransferStatus s;
            try { s = TransferStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown status: " + status); }
            rows = outgoing ? transferRepository.findByFromBranchIdInAndStatusOrderByRequestedAtDesc(ids, s, page)
                            : transferRepository.findByToBranchIdInAndStatusOrderByRequestedAtDesc(ids, s, page);
        }
        Map<Long, String> names = patientRepository.findAllById(rows.stream().map(BranchTransferRequest::getPatientId).distinct().toList())
                .stream().collect(Collectors.toMap(Patient::getId, pt -> pt.getFullName() == null ? "" : pt.getFullName()));
        Map<String, String> branches = branchService.nameMap();
        return rows.stream().map(t -> toDto(t, names.get(t.getPatientId()), branches)).toList();
    }

    /** The rule check the approver sees. Visible to staff of the source or destination branch, and admins. */
    @Transactional(readOnly = true)
    public TransferValidationDTO validationFor(AuthUser user, Long id) {
        requireStaffRole(user);
        BranchTransferRequest t = transferRepository.findById(id).orElseThrow(() -> ApiException.notFound("Transfer request not found."));
        if (!user.isAdmin() && !t.getToBranchId().equals(user.branchId()) && !t.getFromBranchId().equals(user.branchId())) {
            throw ApiException.forbidden("You do not have access to this transfer request.");
        }
        Patient p = patientRepository.findById(t.getPatientId()).orElseThrow(() -> ApiException.notFound("Patient not found."));
        return validate(p, t.getFromBranchId(), t.getToBranchId(), t.getId(), t.getStatus().name());
    }

    @Transactional
    public TransferDTO approve(AuthUser user, Long id, String note, boolean acknowledged) {
        BranchTransferRequest t = loadForDecision(user, id);
        Patient p = patientRepository.findById(t.getPatientId()).orElseThrow(() -> ApiException.notFound("Patient not found."));
        TransferValidationDTO v = validate(p, t.getFromBranchId(), t.getToBranchId(), t.getId(), t.getStatus().name());
        String warnings = enforce(v, acknowledged, note);

        applyMove(p, t.getToBranchId(), user.email(), t.getReason());
        close(t, TransferStatus.APPROVED, user, blankToNull(note), warnings);
        log.info("Transfer {} APPROVED by {} ({}): patient={} {} -> {}, warnings={}", t.getId(), user.email(), user.role(),
                p.getId(), t.getFromBranchId(), t.getToBranchId(), warnings == null ? "none" : "acknowledged");
        return toDto(transferRepository.save(t), p.getFullName(), branchService.nameMap());
    }

    @Transactional
    public TransferDTO reject(AuthUser user, Long id, String note) {
        BranchTransferRequest t = loadForDecision(user, id);
        Patient p = patientRepository.findById(t.getPatientId()).orElse(null);
        close(t, TransferStatus.REJECTED, user, blankToNull(note), null);   // the patient's branch is not touched
        log.info("Transfer {} REJECTED by {} ({}): patient={} {} -> {}", t.getId(), user.email(), user.role(),
                t.getPatientId(), t.getFromBranchId(), t.getToBranchId());
        return toDto(transferRepository.save(t), p == null ? null : p.getFullName(), branchService.nameMap());
    }

    /** Withdraw a pending request: ADMIN, or a receptionist of the source or destination branch. */
    @Transactional
    public TransferDTO cancel(AuthUser user, Long id, String note) {
        requireStaffRole(user);
        BranchTransferRequest t = transferRepository.findById(id).orElseThrow(() -> ApiException.notFound("Transfer request not found."));
        if (!user.isAdmin() && !t.getToBranchId().equals(user.branchId()) && !t.getFromBranchId().equals(user.branchId())) {
            throw ApiException.forbidden("You do not have access to this transfer request.");
        }
        requirePending(t);
        Patient p = patientRepository.findById(t.getPatientId()).orElse(null);
        close(t, TransferStatus.CANCELLED, user, blankToNull(note) == null ? "Cancelled by staff" : note.trim(), null);
        log.info("Transfer {} CANCELLED by {} ({}): patient={}", t.getId(), user.email(), user.role(), t.getPatientId());
        return toDto(transferRepository.save(t), p == null ? null : p.getFullName(), branchService.nameMap());
    }

    /** Administrator moves a patient directly (also covers walk-in patients who have no login). Same rules as an approval. */
    @Transactional
    public TransferDTO adminTransfer(AuthUser user, Long patientId, TransferRequestBody body) {
        if (!user.isAdmin()) throw ApiException.forbidden("Only an administrator can transfer a patient directly.");
        Patient p = patientRepository.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient not found."));
        boolean ack = Boolean.TRUE.equals(body.acknowledgeWarnings());
        return startTransfer(p, body.branchId(), body.reason(), user, true, false, ack);
    }

    /** The pending request of a patient (for the profile screens), or null. */
    @Transactional(readOnly = true)
    public TransferDTO pendingFor(Patient p) {
        return transferRepository.findFirstByPatientIdAndStatus(p.getId(), TransferStatus.PENDING)
                .map(t -> toDto(t, p.getFullName(), branchService.nameMap())).orElse(null);
    }

    // ------------------------------------------------------------------ rules

    /** Evaluates the clinic rules for moving this patient from fromBranch to toBranch right now. */
    TransferValidationDTO validate(Patient p, String fromBranchId, String toBranchId, Long transferId, String status) {
        List<Issue> blockers = new ArrayList<>();
        List<Issue> warnings = new ArrayList<>();

        if (fromBranchId == null || !fromBranchId.equals(p.getBranchId())) {
            blockers.add(new Issue("TRANSFER_STALE", "The patient's branch has changed since this request was made. Reject it and raise a new one."));
        }
        if (!branchService.require(toBranchId).isActive()) {
            blockers.add(new Issue("BRANCH_INACTIVE", "The destination branch is not accepting patients right now."));
        }
        LocalDateTime startOfTomorrow = LocalDate.now().plusDays(1).atStartOfDay();
        long openNow = appointmentRepository.countByPatientIdAndStatusInAndAppointmentDateBefore(p.getId(), OPEN, startOfTomorrow);
        long future = appointmentRepository.countByPatientIdAndStatusInAndAppointmentDateGreaterThanEqual(p.getId(), OPEN, startOfTomorrow);
        long dueCount = billRepository.countByPatientIdAndStatusIn(p.getId(), DUE);
        BigDecimal dueAmount = dueCount == 0 ? BigDecimal.ZERO : billRepository.outstandingBalance(p.getId(), DUE);
        if (dueAmount == null) dueAmount = BigDecimal.ZERO;

        if (openNow > 0) {
            blockers.add(new Issue("OPEN_APPOINTMENT", openNow + " open appointment(s) due today or overdue. Complete or cancel them before the transfer."));
        }
        if (future > 0) {
            warnings.add(new Issue("FUTURE_APPOINTMENT", future + " upcoming appointment(s) stay booked at their original branch and are not moved. Cancel or rebook them if needed."));
        }
        if (dueCount > 0) {
            Issue due = new Issue("OUTSTANDING_INVOICES", dueCount + " invoice(s) with " + dueAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                    + " still due. They stay with their original branch, which keeps collecting them.");
            (blockOnOutstandingInvoices ? blockers : warnings).add(due);
        }
        return new TransferValidationDTO(transferId, status, blockers.isEmpty(), !warnings.isEmpty(), blockers, warnings,
                openNow, future, dueCount, dueAmount);
    }

    /** Throws unless the decision may go ahead; returns the acknowledged warnings (joined) for the audit trail, or null. */
    private String enforce(TransferValidationDTO v, boolean acknowledged, String note) {
        if (!v.blockers().isEmpty()) {
            throw ApiException.conflict("TRANSFER_BLOCKED", v.blockers().stream().map(Issue::message).collect(Collectors.joining(" ")));
        }
        if (!v.requiresAcknowledgement()) return null;
        if (!acknowledged) {
            throw ApiException.conflict("ACKNOWLEDGEMENT_REQUIRED", "Please review the warnings and confirm that you accept them to continue.");
        }
        if (note == null || note.isBlank()) {
            throw ApiException.badRequest("A note is required when approving a transfer that has warnings.");
        }
        String joined = v.warnings().stream().map(Issue::message).collect(Collectors.joining(" | "));
        return joined.length() > 1000 ? joined.substring(0, 1000) : joined;
    }

    // ------------------------------------------------------------------ shared

    private TransferDTO startTransfer(Patient p, String toBranchId, String reason, AuthUser actor, boolean forceImmediate, boolean staffRequest) {
        return startTransfer(p, toBranchId, reason, actor, forceImmediate, staffRequest, false);
    }

    private TransferDTO startTransfer(Patient p, String toBranchId, String reason, AuthUser actor, boolean forceImmediate,
                                      boolean staffRequest, boolean acknowledged) {
        Branch target = branchService.require(toBranchId);
        if (!target.isActive()) throw ApiException.badRequest("That branch is not accepting patients right now.");
        if (target.getId().equals(p.getBranchId())) throw ApiException.badRequest("Source and destination branch must be different. The patient is already registered at this branch.");
        if (transferRepository.findFirstByPatientIdAndStatus(p.getId(), TransferStatus.PENDING).isPresent()) {
            throw ApiException.conflict("TRANSFER_PENDING", "A transfer request is already waiting for approval.");
        }

        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(p.getId());
        t.setFromBranchId(p.getBranchId());
        t.setToBranchId(target.getId());
        t.setReason(blankToNull(reason));
        t.setRequestedBy(actor.email());
        t.setRequestedByRole(actor.role() == null ? null : actor.role().name());

        boolean immediate = forceImmediate || (!approvalRequired && !staffRequest);
        if (immediate) {
            TransferValidationDTO v = validate(p, p.getBranchId(), target.getId(), null, "PENDING");
            // An admin must acknowledge warnings (and give the reason as note); a patient's auto-applied request has no one to ask.
            String warnings = forceImmediate ? enforce(v, acknowledged, t.getReason()) : enforce(v, true, "auto");
            applyMove(p, target.getId(), actor.email(), t.getReason());
            close(t, TransferStatus.APPROVED, actor, forceImmediate ? "Transferred directly by an administrator" : "Applied immediately (approval not required)", warnings);
        }
        BranchTransferRequest saved = transferRepository.save(t);
        log.info("Transfer {} {} by {} ({}): patient={} {} -> {}", saved.getId(), immediate ? "APPLIED" : "REQUESTED",
                actor.email(), actor.role(), p.getId(), saved.getFromBranchId(), saved.getToBranchId());
        return toDto(saved, p.getFullName(), branchService.nameMap());
    }

    /** Close the old branch stint, open the new one, move the patient. Appointments/bills/payments are not touched. */
    private void applyMove(Patient p, String toBranchId, String authorizedBy, String reason) {
        LocalDateTime now = LocalDateTime.now();
        historyRepository.findByPatientIdAndToDateIsNull(p.getId()).ifPresent(h -> {
            h.setToDate(now);
            historyRepository.save(h);
        });
        PatientBranchHistory next = new PatientBranchHistory(p.getId(), toBranchId);
        next.setFromDate(now);
        next.setAuthorizedBy(authorizedBy);
        next.setReason(reason);
        historyRepository.save(next);
        p.setBranchId(toBranchId);
        if (p.getUser() != null) p.getUser().setBranchId(toBranchId); // keep the login row in step (it is returned at login)
        patientRepository.save(p);
    }

    private void close(BranchTransferRequest t, TransferStatus status, AuthUser by, String note, String warnings) {
        t.setStatus(status);
        t.setDecidedBy(by.email());
        t.setDecidedAt(LocalDateTime.now());
        t.setDecisionNote(note);
        t.setDecisionWarnings(warnings);
    }

    /** Loads a request the caller is allowed to APPROVE/REJECT: staff role, receiving branch (or admin), still pending, not their own. */
    private BranchTransferRequest loadForDecision(AuthUser user, Long id) {
        requireStaffRole(user);
        BranchTransferRequest t = transferRepository.findById(id).orElseThrow(() -> ApiException.notFound("Transfer request not found."));
        access.requireBranch(user, t.getToBranchId()); // only the receiving branch (or an admin) decides
        requirePending(t);
        if (!allowSelfApproval && t.getRequestedBy() != null && t.getRequestedBy().equalsIgnoreCase(user.email())) {
            throw ApiException.forbidden("You raised this request, so someone else must approve or reject it.");
        }
        return t;
    }

    private void requirePending(BranchTransferRequest t) {
        if (t.getStatus() != TransferStatus.PENDING) {
            throw ApiException.conflict("TRANSFER_DECIDED", "This request has already been " + t.getStatus().name().toLowerCase() + ".");
        }
    }

    private static void requireStaffRole(AuthUser user) {
        if (user.role() != Role.ADMIN && user.role() != Role.RECEPTIONIST) {
            throw ApiException.forbidden("Only receptionists and administrators can manage branch transfers.");
        }
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private Patient patientOf(AuthUser user) {
        return patientRepository.findByUserEmail(user.email())
                .orElseThrow(() -> new ApiException(404, "PROFILE_NOT_FOUND", "No patient profile is linked to this account."));
    }

    private TransferDTO toDto(BranchTransferRequest t, String patientName, Map<String, String> branchNames) {
        return TransferDTO.of(t, patientName, branchNames.get(t.getFromBranchId()), branchNames.get(t.getToBranchId()));
    }
}
