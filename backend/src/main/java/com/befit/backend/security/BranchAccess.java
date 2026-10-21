package com.befit.backend.security;

import com.befit.backend.entity.AppointmentStatus;
import com.befit.backend.entity.Branch;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.Role;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.AppointmentRepository;
import com.befit.backend.repository.BillRepository;
import com.befit.backend.repository.BranchRepository;
import com.befit.backend.repository.PatientBranchHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * THE place where branch rules are enforced on the server. Controllers/services never trust a branchId
 * coming from the browser: they ask this class what the logged-in user is allowed to see or create.
 *
 *  - RECEPTIONIST / DOCTOR : exactly their own branch (the ?branchId= the frontend sends is ignored)
 *  - ADMIN                 : any branch, or all of them (?branchId=ALL or no value)
 *
 * Patient-profile access (see {@link #patientAccess}) has four levels, strongest first:
 *  - FULL            : ADMIN, or staff of the patient's CURRENT (home) branch. Full profile.
 *  - TEMPORARY_VISIT : staff of a branch that has a live visit (open appointment, or one finished a few days ago)
 *                      for a patient registered elsewhere. Identification + that branch's own visits only.
 *  - HISTORICAL      : staff of a branch the patient used to belong to / was treated or billed at, but not any more.
 *                      Same limited view: identification + that branch's own records only.
 *  - DENIED          : everything else -> 403.
 * Limited levels never receive address, emergency contact, blood group, occupation, medical history, other
 * branches' visits/invoices or the branch-move log.
 */
@Component
public class BranchAccess {
    public static final String ALL = "ALL";
    /** Appointment statuses that mean the patient is (or is about to be) physically being seen. */
    static final Set<AppointmentStatus> OPEN_VISIT_STATUSES = EnumSet.of(
            AppointmentStatus.SCHEDULED, AppointmentStatus.WAITING, AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_PROGRESS);

    public enum PatientAccessLevel {
        FULL, TEMPORARY_VISIT, HISTORICAL, DENIED;
        public boolean isLimited() { return this == TEMPORARY_VISIT || this == HISTORICAL; }
    }

    private final BranchRepository branchRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final PatientBranchHistoryRepository historyRepository;
    /** How long after a visit is COMPLETED the receiving branch keeps temporary-visit access (billing, follow-up). */
    private final int visitGraceDays;

    public BranchAccess(BranchRepository branchRepository, AppointmentRepository appointmentRepository,
                        BillRepository billRepository, PatientBranchHistoryRepository historyRepository,
                        @Value("${befit.access.temporary-visit-grace-days:3}") int visitGraceDays) {
        this.branchRepository = branchRepository;
        this.appointmentRepository = appointmentRepository;
        this.billRepository = billRepository;
        this.historyRepository = historyRepository;
        this.visitGraceDays = Math.max(visitGraceDays, 0);
    }

    /**
     * What may this STAFF user see of this patient's profile? Decided only from the server-side user and the stored
     * patient/visit/branch data - nothing from the request. Patients use their own /me endpoints, not this.
     */
    public PatientAccessLevel patientAccess(AuthUser user, Patient p) {
        if (user.isAdmin()) return PatientAccessLevel.FULL;
        if (user.role() != Role.DOCTOR && user.role() != Role.RECEPTIONIST) return PatientAccessLevel.DENIED;
        String b = user.branchId();
        if (b == null || b.isBlank()) return PatientAccessLevel.DENIED;
        if (b.equals(p.getBranchId())) return PatientAccessLevel.FULL;
        if (appointmentRepository.existsLiveVisit(p.getId(), b, OPEN_VISIT_STATUSES, AppointmentStatus.COMPLETED,
                LocalDateTime.now().minusDays(visitGraceDays))) {
            return PatientAccessLevel.TEMPORARY_VISIT;
        }
        if (!historyRepository.findByPatientIdAndBranchIdInOrderByFromDateDesc(p.getId(), List.of(b)).isEmpty()
                || appointmentRepository.existsByPatientIdAndBranchId(p.getId(), b)
                || billRepository.existsByPatientIdAndBranchId(p.getId(), b)) {
            return PatientAccessLevel.HISTORICAL;
        }
        return PatientAccessLevel.DENIED;
    }

    public AuthUser current() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthUser u)) {
            throw new ApiException(401, "UNAUTHENTICATED", "Please log in to continue.");
        }
        return u;
    }

    /** Branches whose records this user may READ. */
    public List<String> visibleBranchIds(AuthUser user, String requested) {
        if (user.isAdmin()) {
            if (requested == null || requested.isBlank() || ALL.equalsIgnoreCase(requested)) {
                return branchRepository.findAllIds();
            }
            if (!branchRepository.existsById(requested)) throw ApiException.badRequest("Unknown branch: " + requested);
            return List.of(requested);
        }
        return List.of(requireOwnBranch(user));
    }

    /** The single branch a NEW record (appointment, invoice, patient) will be created in. */
    public String branchForCreate(AuthUser user, String requested) {
        if (!user.isAdmin()) return requireOwnBranch(user);
        if (requested == null || requested.isBlank() || ALL.equalsIgnoreCase(requested)) {
            throw ApiException.badRequest("Please choose a branch.");
        }
        Branch b = branchRepository.findById(requested).orElseThrow(() -> ApiException.badRequest("Unknown branch: " + requested));
        if (!b.isActive()) throw ApiException.badRequest("That branch is not active.");
        return b.getId();
    }

    /** Throws 403 unless the user may touch a record that belongs to recordBranchId. */
    public void requireBranch(AuthUser user, String recordBranchId) {
        if (user.isAdmin()) return;
        if (recordBranchId == null || !recordBranchId.equals(user.branchId())) {
            throw ApiException.forbidden("You do not have access to records of another branch.");
        }
    }

    private String requireOwnBranch(AuthUser user) {
        if (user.branchId() == null || user.branchId().isBlank()) {
            throw ApiException.forbidden("Your account is not assigned to a branch. Please contact an administrator.");
        }
        return user.branchId();
    }
}
