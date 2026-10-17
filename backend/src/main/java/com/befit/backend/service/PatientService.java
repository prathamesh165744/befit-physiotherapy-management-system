package com.befit.backend.service;

import com.befit.backend.dto.*;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.PatientBranchHistory;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PatientService {
    private static final Set<String> SORTABLE = Set.of("fullName", "id", "registrationDate");
    private static final int HISTORY_LIMIT = 200;

    private final PatientRepository patientRepository;
    private final PatientBranchHistoryRepository historyRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final BranchRepository branchRepository;
    private final BranchService branchService;
    private final TransferService transferService;
    private final BranchAccess access;

    public PatientService(PatientRepository patientRepository, PatientBranchHistoryRepository historyRepository,
                          AppointmentRepository appointmentRepository, BillRepository billRepository,
                          BranchRepository branchRepository, BranchService branchService,
                          TransferService transferService, BranchAccess access) {
        this.patientRepository = patientRepository;
        this.historyRepository = historyRepository;
        this.appointmentRepository = appointmentRepository;
        this.billRepository = billRepository;
        this.branchRepository = branchRepository;
        this.branchService = branchService;
        this.transferService = transferService;
        this.access = access;
    }

    // ------------------------------------------------------------------ staff

    /**
     * Paged, searchable patient directory. scope=current -> patients now at the branch;
     * scope=all -> also patients who moved away but still have visits/invoices there.
     */
    @Transactional(readOnly = true)
    public PageResponse<PatientSummaryDTO> list(AuthUser user, String branch, String scope, String q,
                                                int page, int size, String sort, String dir) {
        List<String> ids = access.visibleBranchIds(user, branch);
        String sortProp = SORTABLE.contains(sort) ? sort : "id";
        Sort.Direction d = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(d, sortProp));
        String pattern = likePattern(q);
        Page<Patient> result = "all".equalsIgnoreCase(scope)
                ? patientRepository.searchCurrentOrPast(ids, pattern, pageable)
                : patientRepository.searchCurrent(ids, pattern, pageable);
        Map<String, String> names = branchService.nameMap();
        return PageResponse.of(result, p -> PatientSummaryDTO.of(p, names.get(p.getBranchId())));
    }

    /** Small, capped list for dropdowns (e.g. choosing a patient when booking an appointment). Current patients only. */
    @Transactional(readOnly = true)
    public List<PatientSummaryDTO> options(AuthUser user, String branch) {
        List<String> ids = access.visibleBranchIds(user, branch);
        Map<String, String> names = branchService.nameMap();
        return patientRepository.searchCurrent(ids, "%", PageRequest.of(0, 500, Sort.by("fullName"))).getContent().stream()
                .map(p -> PatientSummaryDTO.of(p, names.get(p.getBranchId()))).toList();
    }

    /** Staff opening a patient file (the "View File" button). */
    @Transactional(readOnly = true)
    public PatientProfileDTO getForStaff(AuthUser user, Long id) {
        Patient p = load(id);
        requireStaffAccess(user, p);
        return toProfile(p);
    }

    /** Visits, invoices and branch moves of a patient - limited to the branches the caller may see. */
    @Transactional(readOnly = true)
    public PatientHistoryDTO historyForStaff(AuthUser user, Long id) {
        Patient p = load(id);
        requireStaffAccess(user, p);
        return buildHistory(p, access.visibleBranchIds(user, null));
    }

    @Transactional
    public PatientSummaryDTO register(AuthUser user, PatientRegistrationRequest r) {
        String branchId = access.branchForCreate(user, r.branchId());
        Patient p = new Patient();
        p.setFullName(r.fullName().trim());
        p.setPhone(r.phone().trim());
        p.setGender(normalizeGender(r.gender()));
        p.setDateOfBirth(r.dateOfBirth());
        p.setEmergencyContact(blankToNull(r.emergencyContact()));
        p.setEmergencyContactName(blankToNull(r.emergencyContactName()));
        p.setAddress(blankToNull(r.address()));
        p.setBranchId(branchId);
        p.setRegistrationDate(LocalDate.now());
        Patient saved = patientRepository.save(p);

        PatientBranchHistory first = new PatientBranchHistory(saved.getId(), branchId);
        first.setAuthorizedBy(user.email());
        first.setReason("Registered at branch");
        historyRepository.save(first);
        return PatientSummaryDTO.of(saved, branchService.nameMap().get(branchId));
    }

    /**
     * A staff member may open a patient if the patient is at their branch NOW, or was treated/billed there in the past
     * (so the old branch can still see its own records). Admins may open anyone. Otherwise 403.
     */
    public void requireStaffAccess(AuthUser user, Patient p) {
        if (user.isAdmin()) return;
        String b = user.branchId();
        if (b == null || b.isBlank()) {
            throw ApiException.forbidden("Your account is not assigned to a branch. Please contact an administrator.");
        }
        if (b.equals(p.getBranchId())
                || appointmentRepository.existsByPatientIdAndBranchId(p.getId(), b)
                || billRepository.existsByPatientIdAndBranchId(p.getId(), b)) {
            return;
        }
        throw ApiException.forbidden("This patient is not registered at your branch.");
    }

    public Patient load(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> ApiException.notFound("Patient not found."));
    }

    // ------------------------------------------------------------------ the logged-in patient

    @Transactional(readOnly = true)
    public PatientProfileDTO myProfile(AuthUser user) {
        return toProfile(mine(user));
    }

    @Transactional
    public PatientProfileDTO updateMyProfile(AuthUser user, PatientProfileUpdate u) {
        Patient p = mine(user);
        if (u.getFullName() != null && !u.getFullName().isBlank()) p.setFullName(u.getFullName().trim());
        if (u.getPhone() != null && !u.getPhone().isBlank()) p.setPhone(u.getPhone().trim());
        if (u.getGender() != null) p.setGender(normalizeGender(u.getGender()));
        if (u.getAddress() != null) p.setAddress(u.getAddress());
        if (u.getEmergencyContact() != null) p.setEmergencyContact(u.getEmergencyContact());
        if (u.getEmergencyContactName() != null) p.setEmergencyContactName(u.getEmergencyContactName());
        if (u.getBloodGroup() != null) p.setBloodGroup(u.getBloodGroup());
        if (u.getOccupation() != null) p.setOccupation(u.getOccupation());
        return toProfile(patientRepository.save(p));
    }

    /** A patient sees their OWN history across every branch, each row labelled with the branch where it happened. */
    @Transactional(readOnly = true)
    public PatientHistoryDTO myHistory(AuthUser user) {
        return buildHistory(mine(user), branchRepository.findAllIds());
    }

    // ------------------------------------------------------------------ helpers

    private Patient mine(AuthUser user) {
        return patientRepository.findByUserEmail(user.email())
                .orElseThrow(() -> new ApiException(404, "PROFILE_NOT_FOUND", "No patient profile is linked to this account."));
    }

    private PatientProfileDTO toProfile(Patient p) {
        return PatientProfileDTO.of(p, branchService.nameMap().get(p.getBranchId()), transferService.pendingFor(p));
    }

    private PatientHistoryDTO buildHistory(Patient p, List<String> branchIds) {
        Map<String, String> names = branchService.nameMap();
        PageRequest limit = PageRequest.of(0, HISTORY_LIMIT);
        var appts = appointmentRepository.findByPatientIdAndBranchIdInOrderByAppointmentDateDesc(p.getId(), branchIds, limit).stream()
                .map(a -> AppointmentDTO.of(a, names.get(a.getBranchId()))).toList();
        var bills = billRepository.findByPatientIdAndBranchIdInOrderByBillDateDesc(p.getId(), branchIds, limit).stream()
                .map(b -> BillDTO.of(b, names.get(b.getBranchId()))).toList();
        var stints = historyRepository.findByPatientIdAndBranchIdInOrderByFromDateDesc(p.getId(), branchIds).stream()
                .map(h -> new PatientHistoryDTO.BranchStint(h.getBranchId(), names.get(h.getBranchId()), h.getFromDate(),
                        h.getToDate(), h.getAuthorizedBy(), h.getReason())).toList();
        return new PatientHistoryDTO(appts, bills, stints);
    }

    /** Turns the user's search text into a lower-case LIKE pattern. Never null/empty (see BillRepository.FILTER). */
    static String likePattern(String q) {
        if (q == null || q.isBlank()) return "%";
        String t = q.trim().toLowerCase();
        if (t.startsWith("reg-")) t = t.substring(4);
        return "%" + t + "%";
    }

    static String normalizeGender(String g) {
        if (g == null || g.isBlank()) return null;
        return switch (g.trim().toUpperCase()) {
            case "M", "MALE" -> "Male";
            case "F", "FEMALE" -> "Female";
            case "O", "OTHER" -> "Other";
            default -> g.trim();
        };
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
