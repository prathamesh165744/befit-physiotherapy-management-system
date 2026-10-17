package com.befit.backend.service;

import com.befit.backend.dto.TransferDTO;
import com.befit.backend.dto.TransferRequestBody;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Patient branch transfers.
 *
 * What a transfer changes:  patients.branch_id + a new patient_branch_history row.
 * What a transfer NEVER touches: appointments, bills, payments. They keep the branch_id they were created with,
 *                                so the old branch keeps its history and the new branch starts clean.
 *
 * befit.transfer.approval-required=true  (default): patient asks -> receptionist of the DESTINATION branch (or an admin) approves.
 * befit.transfer.approval-required=false          : the patient's choice is applied immediately (the old behaviour).
 * Either way a branch_transfer_requests row is written, so every move is auditable.
 */
@Service
public class TransferService {
    private static final EnumSet<AppointmentStatus> OPEN = EnumSet.of(AppointmentStatus.SCHEDULED, AppointmentStatus.WAITING,
            AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_PROGRESS);

    private final PatientRepository patientRepository;
    private final PatientBranchHistoryRepository historyRepository;
    private final BranchTransferRequestRepository transferRepository;
    private final AppointmentRepository appointmentRepository;
    private final BranchService branchService;
    private final BranchAccess access;

    @Value("${befit.transfer.approval-required:true}")
    private boolean approvalRequired;

    public TransferService(PatientRepository patientRepository, PatientBranchHistoryRepository historyRepository,
                           BranchTransferRequestRepository transferRepository, AppointmentRepository appointmentRepository,
                           BranchService branchService, BranchAccess access) {
        this.patientRepository = patientRepository;
        this.historyRepository = historyRepository;
        this.transferRepository = transferRepository;
        this.appointmentRepository = appointmentRepository;
        this.branchService = branchService;
        this.access = access;
    }

    // ------------------------------------------------------------------ patient side

    @Transactional
    public TransferDTO requestTransfer(AuthUser user, TransferRequestBody body) {
        Patient p = patientOf(user);
        return startTransfer(p, body.branchId(), body.reason(), user.email());
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
        t.setStatus(TransferStatus.CANCELLED);
        t.setDecidedBy(user.email());
        t.setDecidedAt(LocalDateTime.now());
        t.setDecisionNote("Cancelled by the patient");
        return toDto(transferRepository.save(t), p.getFullName(), branchService.nameMap());
    }

    // ------------------------------------------------------------------ staff side

    @Transactional(readOnly = true)
    public List<TransferDTO> listForStaff(AuthUser user, String branch, String status) {
        List<String> ids = access.visibleBranchIds(user, branch);
        PageRequest page = PageRequest.of(0, 100);
        List<BranchTransferRequest> rows;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            rows = transferRepository.findByToBranchIdInOrderByRequestedAtDesc(ids, page);
        } else {
            TransferStatus s;
            try { s = TransferStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown status: " + status); }
            rows = transferRepository.findByToBranchIdInAndStatusOrderByRequestedAtDesc(ids, s, page);
        }
        Map<Long, String> names = patientRepository.findAllById(rows.stream().map(BranchTransferRequest::getPatientId).distinct().toList())
                .stream().collect(Collectors.toMap(Patient::getId, pt -> pt.getFullName() == null ? "" : pt.getFullName()));
        Map<String, String> branches = branchService.nameMap();
        return rows.stream().map(t -> toDto(t, names.get(t.getPatientId()), branches)).toList();
    }

    @Transactional
    public TransferDTO approve(AuthUser user, Long id, String note) {
        BranchTransferRequest t = loadPending(user, id);
        Patient p = patientRepository.findById(t.getPatientId()).orElseThrow(() -> ApiException.notFound("Patient not found."));
        if (!t.getFromBranchId().equals(p.getBranchId())) {
            throw ApiException.conflict("TRANSFER_STALE", "The patient's branch has changed since this request was made.");
        }
        requireActiveBranch(t.getToBranchId());
        requireNoOpenVisits(p);
        applyMove(p, t.getToBranchId(), user.email(), t.getReason());
        t.setStatus(TransferStatus.APPROVED);
        t.setDecidedBy(user.email());
        t.setDecidedAt(LocalDateTime.now());
        t.setDecisionNote(note);
        return toDto(transferRepository.save(t), p.getFullName(), branchService.nameMap());
    }

    @Transactional
    public TransferDTO reject(AuthUser user, Long id, String note) {
        BranchTransferRequest t = loadPending(user, id);
        Patient p = patientRepository.findById(t.getPatientId()).orElse(null);
        t.setStatus(TransferStatus.REJECTED);
        t.setDecidedBy(user.email());
        t.setDecidedAt(LocalDateTime.now());
        t.setDecisionNote(note);
        return toDto(transferRepository.save(t), p == null ? null : p.getFullName(), branchService.nameMap());
    }

    /** Administrator moves a patient directly (also covers walk-in patients who have no login). */
    @Transactional
    public TransferDTO adminTransfer(AuthUser user, Long patientId, TransferRequestBody body) {
        if (!user.isAdmin()) throw ApiException.forbidden("Only an administrator can transfer a patient directly.");
        Patient p = patientRepository.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient not found."));
        return startTransfer(p, body.branchId(), body.reason(), user.email(), true);
    }

    /** The pending request of a patient (for the profile screens), or null. */
    @Transactional(readOnly = true)
    public TransferDTO pendingFor(Patient p) {
        return transferRepository.findFirstByPatientIdAndStatus(p.getId(), TransferStatus.PENDING)
                .map(t -> toDto(t, p.getFullName(), branchService.nameMap())).orElse(null);
    }

    // ------------------------------------------------------------------ shared

    private TransferDTO startTransfer(Patient p, String toBranchId, String reason, String actor) {
        return startTransfer(p, toBranchId, reason, actor, false);
    }

    private TransferDTO startTransfer(Patient p, String toBranchId, String reason, String actor, boolean forceImmediate) {
        Branch target = branchService.require(toBranchId);
        if (!target.isActive()) throw ApiException.badRequest("That branch is not accepting patients right now.");
        if (target.getId().equals(p.getBranchId())) throw ApiException.badRequest("The patient is already registered at this branch.");
        if (transferRepository.findFirstByPatientIdAndStatus(p.getId(), TransferStatus.PENDING).isPresent()) {
            throw ApiException.conflict("TRANSFER_PENDING", "A transfer request is already waiting for approval.");
        }
        requireNoOpenVisits(p);

        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(p.getId());
        t.setFromBranchId(p.getBranchId());
        t.setToBranchId(target.getId());
        t.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        t.setRequestedBy(actor);

        if (forceImmediate || !approvalRequired) {
            applyMove(p, target.getId(), actor, t.getReason());
            t.setStatus(TransferStatus.APPROVED);
            t.setDecidedBy(actor);
            t.setDecidedAt(LocalDateTime.now());
            t.setDecisionNote(forceImmediate ? "Transferred directly by an administrator" : "Applied immediately (approval not required)");
        }
        return toDto(transferRepository.save(t), p.getFullName(), branchService.nameMap());
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

    private BranchTransferRequest loadPending(AuthUser user, Long id) {
        BranchTransferRequest t = transferRepository.findById(id).orElseThrow(() -> ApiException.notFound("Transfer request not found."));
        access.requireBranch(user, t.getToBranchId()); // only the receiving branch (or an admin) decides
        if (t.getStatus() != TransferStatus.PENDING) {
            throw ApiException.conflict("TRANSFER_DECIDED", "This request has already been " + t.getStatus().name().toLowerCase() + ".");
        }
        return t;
    }

    private void requireActiveBranch(String branchId) {
        if (!branchService.require(branchId).isActive()) throw ApiException.badRequest("That branch is not accepting patients right now.");
    }

    private void requireNoOpenVisits(Patient p) {
        if (appointmentRepository.existsByPatientIdAndStatusIn(p.getId(), OPEN)) {
            throw ApiException.conflict("OPEN_APPOINTMENT",
                    "There is an open appointment at the current branch. It must be completed or cancelled before the transfer.");
        }
    }

    private Patient patientOf(AuthUser user) {
        return patientRepository.findByUserEmail(user.email())
                .orElseThrow(() -> new ApiException(404, "PROFILE_NOT_FOUND", "No patient profile is linked to this account."));
    }

    private TransferDTO toDto(BranchTransferRequest t, String patientName, Map<String, String> branchNames) {
        return TransferDTO.of(t, patientName, branchNames.get(t.getFromBranchId()), branchNames.get(t.getToBranchId()));
    }
}
