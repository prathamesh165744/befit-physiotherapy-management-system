package com.befit.backend.service;

import com.befit.backend.dto.*;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.PatientBranchHistory;
import com.befit.backend.entity.Role;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.security.BranchAccess.PatientAccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PatientService {
    private static final Set<String> SORTABLE = Set.of("fullName", "id", "registrationDate");
    private static final int HISTORY_LIMIT = 200;
    private static final Logger log = LoggerFactory.getLogger(PatientService.class);
    /** Cross-branch search returns at most this many rows, so it can never be used to dump the patient table. */
    private static final int CROSS_SEARCH_LIMIT = 10;
    private static final int MIN_SEARCH_PHONE_DIGITS = 10;

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

    /**
     * Staff opening a patient file (the "View File" button). Home-branch staff and admins get the full profile;
     * staff of another branch with a live visit (or past dealings) get the limited profile; anyone else gets 403.
     */
    @Transactional(readOnly = true)
    public PatientProfileDTO getForStaff(AuthUser user, Long id) {
        Patient p = load(id);
        PatientAccessLevel level = requireStaffAccess(user, p);
        if (level.isLimited()) {
            log.info("Limited patient profile viewed by {} (branch {}): patient={}, access={}", user.email(), user.branchId(), p.getId(), level);
            return PatientProfileDTO.limited(p, branchService.nameMap().get(p.getBranchId()), level.name());
        }
        return toProfile(p, PatientProfileDTO.FULL);
    }

    /**
     * Visits, invoices and branch moves of a patient - limited to the branches the caller may see.
     * In a limited view: only the caller's own branch's visits, invoices only for receptionists, and no branch-move log.
     */
    @Transactional(readOnly = true)
    public PatientHistoryDTO historyForStaff(AuthUser user, Long id) {
        Patient p = load(id);
        PatientAccessLevel level = requireStaffAccess(user, p);
        List<String> branches = access.visibleBranchIds(user, null);
        if (level.isLimited()) {
            return buildHistory(p, branches, user.role() == Role.RECEPTIONIST, false);
        }
        return buildHistory(p, branches, true, true);
    }

    /**
     * Cross-branch "does this person already exist?" lookup, used before registering a new patient.
     * Only ADMIN and RECEPTIONIST may call it. It needs an exact identifier (REG id, full phone number, or full name +
     * date of birth) - free-text browsing of other branches is not possible - returns at most 10 rows, and each row
     * holds only limited identifying data (see CrossBranchPatientDTO). If several identifiers are given, a patient must match all.
     */
    @Transactional(readOnly = true)
    public List<CrossBranchPatientDTO> searchAcrossBranches(AuthUser user, String regId, String phone,
                                                            String fullName, LocalDate dateOfBirth) {
        requireCrossBranchRole(user);

        boolean hasReg = regId != null && !regId.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasName = fullName != null && !fullName.isBlank();
        if (!hasReg && !hasPhone && !hasName && dateOfBirth == null) {
            throw ApiException.badRequest("Enter a phone number, a REG ID, or a full name with date of birth to search.");
        }
        if (hasName != (dateOfBirth != null)) {
            throw ApiException.badRequest("To search by name, enter both the full name and the date of birth.");
        }
        if (dateOfBirth != null && !dateOfBirth.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Date of birth must be in the past.");
        }

        List<String> criteria = new ArrayList<>();
        Map<Long, Patient> matches = null;

        if (hasReg) {
            criteria.add("regId");
            Long id = parseRegId(regId);
            Map<Long, Patient> m = new LinkedHashMap<>();
            patientRepository.findById(id).ifPresent(p -> m.put(p.getId(), p));
            matches = m;
        }
        if (hasPhone) {
            criteria.add("phone");
            String digits = digitsOf(phone);
            if (digits.length() < MIN_SEARCH_PHONE_DIGITS) {
                throw ApiException.badRequest("Enter the full phone number (at least " + MIN_SEARCH_PHONE_DIGITS + " digits).");
            }
            matches = intersect(matches, patientRepository.findByPhonePattern(phonePattern(digits), PageRequest.of(0, CROSS_SEARCH_LIMIT)));
        }
        if (hasName) {
            criteria.add("name+dob");
            matches = intersect(matches, patientRepository.findByNameAndDob(normalizeName(fullName), dateOfBirth, PageRequest.of(0, CROSS_SEARCH_LIMIT)));
        }

        Map<String, String> names = branchService.nameMap();
        List<CrossBranchPatientDTO> out = matches.values().stream().limit(CROSS_SEARCH_LIMIT)
                .map(p -> CrossBranchPatientDTO.of(p, names.get(p.getBranchId()), canOpenFile(user, p)))
                .toList();
        // Audit trail without personal data: who searched, with which kind of identifier, how many matched.
        log.info("Cross-branch patient search by {} (branch {}): criteria={}, matches={}", user.email(), user.branchId(), criteria, out.size());
        return out;
    }

    @Transactional
    public PatientSummaryDTO register(AuthUser user, PatientRegistrationRequest r) {
        String branchId = access.branchForCreate(user, r.branchId());
        String phone = r.phone().trim();
        if (digitsOf(phone).length() < 7) throw ApiException.badRequest("Enter a valid phone number");
        rejectDuplicate(user, r.fullName(), phone, r.dateOfBirth());
        Patient p = new Patient();
        p.setFullName(r.fullName().trim());
        p.setPhone(phone);
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
     * Throws 403 unless the user may open this patient's file, and returns HOW MUCH they may see (see
     * BranchAccess.patientAccess). Other services (billing, appointments) call this too, so one rule applies everywhere.
     */
    public PatientAccessLevel requireStaffAccess(AuthUser user, Patient p) {
        PatientAccessLevel level = access.patientAccess(user, p);
        if (level == PatientAccessLevel.DENIED) {
            if (!user.isAdmin() && (user.branchId() == null || user.branchId().isBlank())) {
                throw ApiException.forbidden("Your account is not assigned to a branch. Please contact an administrator.");
            }
            throw ApiException.forbidden("This patient is not registered at your branch.");
        }
        return level;
    }

    // ------------------------------------------------------------------ cross-branch helpers

    private void requireCrossBranchRole(AuthUser user) {
        if (user.role() != Role.ADMIN && user.role() != Role.RECEPTIONIST) {
            throw ApiException.forbidden("Only receptionists and administrators can search for patients across branches.");
        }
        if (!user.isAdmin() && (user.branchId() == null || user.branchId().isBlank())) {
            throw ApiException.forbidden("Your account is not assigned to a branch. Please contact an administrator.");
        }
    }

    /** May this user open the patient's file (full or limited)? Same rule as the profile endpoint. */
    private boolean canOpenFile(AuthUser user, Patient p) {
        return access.patientAccess(user, p) != PatientAccessLevel.DENIED;
    }

    /**
     * Blocks registering a person who already exists in ANY branch:
     * same full name + same phone number, or same full name + same date of birth.
     * Only ADMIN / RECEPTIONIST are told who and where; anyone else gets a generic message.
     */
    private void rejectDuplicate(AuthUser user, String fullName, String phone, LocalDate dob) {
        String name = normalizeName(fullName);
        List<Patient> dups = patientRepository.findByNameAndPhonePattern(name, phonePattern(digitsOf(phone)), PageRequest.of(0, 1));
        if (dups.isEmpty() && dob != null) {
            dups = patientRepository.findByNameAndDob(name, dob, PageRequest.of(0, 1));
        }
        if (dups.isEmpty()) return;

        Patient existing = dups.get(0);
        boolean mayKnow = user.role() == Role.ADMIN || user.role() == Role.RECEPTIONIST;
        if (!mayKnow) {
            throw ApiException.conflict("DUPLICATE_PATIENT", "This patient is already registered. A new registration cannot be created.");
        }
        String branch = branchService.nameMap().get(existing.getBranchId());
        boolean here = existing.getBranchId() != null && existing.getBranchId().equals(user.branchId());
        throw ApiException.conflict("DUPLICATE_PATIENT", "This patient is already registered as REG-" + existing.getId()
                + (here ? " at your branch." : " at " + (branch == null ? "another branch" : branch) + ".")
                + " Please search for the existing patient instead of creating a new registration.");
    }

    private static Long parseRegId(String regId) {
        String t = regId.trim().toUpperCase();
        if (t.startsWith("REG-")) t = t.substring(4);
        if (!t.matches("\\d{1,18}")) throw ApiException.badRequest("Enter a valid REG ID, for example REG-1024.");
        return Long.parseLong(t);
    }

    private static Map<Long, Patient> intersect(Map<Long, Patient> current, List<Patient> found) {
        Map<Long, Patient> next = new LinkedHashMap<>();
        for (Patient p : found) {
            if (current == null || current.containsKey(p.getId())) next.put(p.getId(), p);
        }
        return next;
    }

    static String digitsOf(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }

    /** Last 10 digits as a suffix match (so +91 prefixes do not matter); shorter numbers must match exactly. */
    static String phonePattern(String digits) {
        return digits.length() >= 10 ? "%" + digits.substring(digits.length() - 10) : digits;
    }

    static String normalizeName(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(); }

    public Patient load(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> ApiException.notFound("Patient not found."));
    }

    // ------------------------------------------------------------------ the logged-in patient

    @Transactional(readOnly = true)
    public PatientProfileDTO myProfile(AuthUser user) {
        return toProfile(mine(user), PatientProfileDTO.SELF);
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
        return toProfile(patientRepository.save(p), PatientProfileDTO.SELF);
    }

    /** A patient sees their OWN history across every branch, each row labelled with the branch where it happened. */
    @Transactional(readOnly = true)
    public PatientHistoryDTO myHistory(AuthUser user) {
        return buildHistory(mine(user), branchRepository.findAllIds(), true, true);
    }

    // ------------------------------------------------------------------ helpers

    private Patient mine(AuthUser user) {
        return patientRepository.findByUserEmail(user.email())
                .orElseThrow(() -> new ApiException(404, "PROFILE_NOT_FOUND", "No patient profile is linked to this account."));
    }

    private PatientProfileDTO toProfile(Patient p, String accessLevel) {
        return PatientProfileDTO.of(p, branchService.nameMap().get(p.getBranchId()), transferService.pendingFor(p), accessLevel);
    }

    private PatientHistoryDTO buildHistory(Patient p, List<String> branchIds, boolean includeBills, boolean includeBranchMoves) {
        Map<String, String> names = branchService.nameMap();
        PageRequest limit = PageRequest.of(0, HISTORY_LIMIT);
        var appts = appointmentRepository.findByPatientIdAndBranchIdInOrderByAppointmentDateDesc(p.getId(), branchIds, limit).stream()
                .map(a -> AppointmentDTO.of(a, names.get(a.getBranchId()))).toList();
        var bills = !includeBills ? List.<BillDTO>of()
                : billRepository.findByPatientIdAndBranchIdInOrderByBillDateDesc(p.getId(), branchIds, limit).stream()
                .map(b -> BillDTO.of(b, names.get(b.getBranchId()))).toList();
        var stints = !includeBranchMoves ? List.<PatientHistoryDTO.BranchStint>of()
                : historyRepository.findByPatientIdAndBranchIdInOrderByFromDateDesc(p.getId(), branchIds).stream()
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