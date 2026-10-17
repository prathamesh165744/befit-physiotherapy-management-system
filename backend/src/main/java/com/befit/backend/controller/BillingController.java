package com.befit.backend.controller;

import com.befit.backend.dto.*;
import com.befit.backend.entity.BillStatus;
import com.befit.backend.exception.ApiException;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Billing API. All endpoints need role ADMIN or RECEPTIONIST (SecurityConfig) and are branch-checked in BillingService.
 *
 *  GET    /api/billing                 paged list (filters: q, status, from, to, page, size, sort, dir)
 *  GET    /api/billing/report          every record matching the same filters (for "Print All")
 *  GET    /api/billing/export          same records as an .xlsx file
 *  GET    /api/billing/{id}            one invoice with items, payments, branch, patient (for print / edit)
 *  GET    /api/billing/{id}/audit      change history
 *  PUT    /api/billing/{id}            edit items / discount / tax (reason required)
 *  POST   /api/billing/{id}/payments   record a payment (idempotent)
 *  POST   /api/billing/{id}/cancel     ADMIN: cancel an invoice (reason required)
 *  POST   /api/billing/manual          create an invoice not linked to an appointment
 *  POST   /api/billing/appointment/{id}  bill an appointment
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final BillingService billingService;
    private final BranchAccess access;

    public BillingController(BillingService billingService, BranchAccess access) {
        this.billingService = billingService;
        this.access = access;
    }

    @GetMapping
    public PageResponse<BillDTO> list(@RequestParam(required = false) String branchId,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(defaultValue = "billDate") String sort,
                                      @RequestParam(defaultValue = "desc") String dir) {
        return billingService.list(access.current(), branchId, filter(q, status, from, to), page, size, sort, dir);
    }

    @GetMapping("/report")
    public BillReportDTO report(@RequestParam(required = false) String branchId,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String from,
                                @RequestParam(required = false) String to) {
        return billingService.report(access.current(), branchId, filter(q, status, from, to));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String branchId,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to) {
        BillingService.ExportFile file = billingService.export(access.current(), branchId, filter(q, status, from, to));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(XLSX)
                .body(file.bytes());
    }

    @GetMapping("/{id}")
    public BillDetailDTO detail(@PathVariable Long id) {
        return billingService.detail(access.current(), id);
    }

    @GetMapping("/{id}/audit")
    public List<BillAuditDTO> audit(@PathVariable Long id) {
        return billingService.audit(access.current(), id);
    }

    @PutMapping("/{id}")
    public BillDetailDTO update(@PathVariable Long id, @Valid @RequestBody BillRequests.Update request) {
        return billingService.update(access.current(), id, request);
    }

    @PostMapping("/{id}/payments")
    public BillDetailDTO addPayment(@PathVariable Long id, @Valid @RequestBody BillRequests.Payment request) {
        return billingService.addPayment(access.current(), id, request);
    }

    @PostMapping("/{id}/cancel")
    public BillDetailDTO cancel(@PathVariable Long id, @Valid @RequestBody BillRequests.Cancel request) {
        return billingService.cancel(access.current(), id, request.reason());
    }

    @PostMapping("/manual")
    public BillDetailDTO createManual(@Valid @RequestBody BillRequests.Create request) {
        return billingService.createManual(access.current(), request);
    }

    @PostMapping("/appointment/{id}")
    public BillDetailDTO generateAppointmentBill(@PathVariable Long id, @Valid @RequestBody BillRequests.Create request) {
        return billingService.createForAppointment(access.current(), id, request);
    }

    // ------------------------------------------------------------------

    private BillingService.Filter filter(String q, String status, String from, String to) {
        BillStatus s = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try { s = BillStatus.valueOf(status.trim().toUpperCase()); }
            catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown status: " + status); }
        }
        LocalDate f = date(from), t = date(to);
        if (f != null && t != null && f.isAfter(t)) throw ApiException.badRequest("'From' date cannot be after 'To' date.");
        return new BillingService.Filter(q, s, f, t);
    }

    private LocalDate date(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); }
        catch (RuntimeException e) { throw ApiException.badRequest("Dates must look like 2026-10-31."); }
    }
}
