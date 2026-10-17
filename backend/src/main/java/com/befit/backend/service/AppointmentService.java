package com.befit.backend.service;

import com.befit.backend.dto.AppointmentCreateRequest;
import com.befit.backend.dto.AppointmentDTO;
import com.befit.backend.dto.PageResponse;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.AppointmentRepository;
import com.befit.backend.repository.UserRepository;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AppointmentService {
    private static final Set<AppointmentStatus> FREE_SLOT_STATUSES =
            EnumSet.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW, AppointmentStatus.COMPLETED);

    /** Appointment status lifecycle. Finished states (COMPLETED / CANCELLED / NO_SHOW) cannot change again. */
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> TRANSITIONS = new EnumMap<>(AppointmentStatus.class);
    static {
        TRANSITIONS.put(AppointmentStatus.SCHEDULED, EnumSet.of(AppointmentStatus.WAITING, AppointmentStatus.CHECKED_IN, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW));
        TRANSITIONS.put(AppointmentStatus.WAITING, EnumSet.of(AppointmentStatus.CHECKED_IN, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW));
        TRANSITIONS.put(AppointmentStatus.CHECKED_IN, EnumSet.of(AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED));
        TRANSITIONS.put(AppointmentStatus.IN_PROGRESS, EnumSet.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED));
        TRANSITIONS.put(AppointmentStatus.COMPLETED, EnumSet.noneOf(AppointmentStatus.class));
        TRANSITIONS.put(AppointmentStatus.CANCELLED, EnumSet.noneOf(AppointmentStatus.class));
        TRANSITIONS.put(AppointmentStatus.NO_SHOW, EnumSet.noneOf(AppointmentStatus.class));
    }

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final PatientService patientService;
    private final BranchService branchService;
    private final BranchAccess access;

    public AppointmentService(AppointmentRepository appointmentRepository, UserRepository userRepository,
                              PatientService patientService, BranchService branchService, BranchAccess access) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.patientService = patientService;
        this.branchService = branchService;
        this.access = access;
    }

    /** Paged list for the Appointments / History screens. date = one day (yyyy-MM-dd), statuses = comma separated. */
    @Transactional(readOnly = true)
    public PageResponse<AppointmentDTO> list(AuthUser user, String branch, String date, String statuses,
                                             int page, int size, String dir) {
        List<String> ids = access.visibleBranchIds(user, branch);
        LocalDateTime from = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2999, 12, 31, 0, 0);
        if (date != null && !date.isBlank()) {
            LocalDate d = parseDate(date);
            from = d.atStartOfDay();
            to = d.plusDays(1).atStartOfDay();
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(direction, "appointmentDate").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Appointment> result = appointmentRepository.search(ids, from, to, parseStatuses(statuses), pageable);
        Map<String, String> names = branchService.nameMap();
        return PageResponse.of(result, a -> AppointmentDTO.of(a, names.get(a.getBranchId())));
    }

    /** A patient's appointments, limited to what the caller may see. */
    @Transactional(readOnly = true)
    public List<AppointmentDTO> forPatient(AuthUser user, Long patientId) {
        Patient p = patientService.load(patientId);
        patientService.requireStaffAccess(user, p);
        Map<String, String> names = branchService.nameMap();
        return appointmentRepository.findByPatientIdAndBranchIdInOrderByAppointmentDateDesc(
                        patientId, access.visibleBranchIds(user, null), PageRequest.of(0, 200)).stream()
                .map(a -> AppointmentDTO.of(a, names.get(a.getBranchId()))).toList();
    }

    /**
     * Books an appointment at the patient's CURRENT branch. A patient who has just transferred is therefore
     * booked at the new branch, while their old appointments stay at the old one.
     */
    @Transactional
    public AppointmentDTO create(AuthUser user, AppointmentCreateRequest r) {
        Patient patient = patientService.load(r.patientId());
        String branchId = patient.getBranchId();
        if (!user.isAdmin() && (branchId == null || !branchId.equals(user.branchId()))) {
            throw ApiException.forbidden("This patient is registered at another branch. Appointments can only be booked at the patient's own branch.");
        }
        Branch branch = branchService.require(branchId);
        if (!branch.isActive()) throw ApiException.badRequest("That branch is not accepting appointments right now.");

        LocalDateTime when;
        try { when = LocalDateTime.parse(r.appointmentDate()); }
        catch (RuntimeException e) { throw ApiException.badRequest("Appointment date/time is not valid."); }
        if (when.toLocalDate().isBefore(LocalDate.now())) throw ApiException.badRequest("An appointment cannot be booked in the past.");

        if (r.doctorId() != null) {
            userRepository.findById(r.doctorId()).ifPresent(doc -> {
                if (doc.getRole() != Role.DOCTOR) throw ApiException.badRequest("The selected person is not a doctor.");
                if (doc.getBranchId() != null && !doc.getBranchId().equals(branchId)) {
                    throw ApiException.badRequest("That doctor works at another branch.");
                }
            });
            if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndStatusNotIn(r.doctorId(), when, FREE_SLOT_STATUSES)) {
                throw ApiException.conflict("SLOT_TAKEN", "That doctor already has an appointment at this time. Please choose another slot.");
            }
        }

        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setDoctorId(r.doctorId());
        a.setDoctorName(r.doctorName().trim());
        a.setCaseType(r.caseType() == null || r.caseType().isBlank() ? "Consultation" : r.caseType().trim());
        a.setAppointmentDate(when);
        a.setBranchId(branchId);
        a.setStatus(AppointmentStatus.WAITING);
        a.setCheckInTime(LocalDateTime.now());
        return AppointmentDTO.of(appointmentRepository.save(a), branch.getName());
    }

    @Transactional
    public AppointmentDTO updateStatus(AuthUser user, Long id, String statusText) {
        Appointment a = appointmentRepository.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Appointment not found."));
        access.requireBranch(user, a.getBranchId());
        AppointmentStatus next;
        try { next = AppointmentStatus.valueOf(statusText == null ? "" : statusText.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown status."); }
        AppointmentStatus current = a.getStatus() == null ? AppointmentStatus.WAITING : a.getStatus();
        if (current == next) return AppointmentDTO.of(a, branchService.nameMap().get(a.getBranchId()));
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw ApiException.conflict("BAD_TRANSITION", "An appointment that is " + current.name().replace('_', ' ').toLowerCase()
                    + " cannot be changed to " + next.name().replace('_', ' ').toLowerCase() + ".");
        }
        a.setStatus(next);
        if (next == AppointmentStatus.CHECKED_IN) a.setCheckInTime(LocalDateTime.now());
        return AppointmentDTO.of(appointmentRepository.save(a), branchService.nameMap().get(a.getBranchId()));
    }

    private List<AppointmentStatus> parseStatuses(String csv) {
        if (csv == null || csv.isBlank()) return List.of(AppointmentStatus.values());
        List<AppointmentStatus> out = new ArrayList<>();
        for (String s : csv.split(",")) {
            try { out.add(AppointmentStatus.valueOf(s.trim().toUpperCase())); }
            catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown status: " + s.trim()); }
        }
        return out.isEmpty() ? List.of(AppointmentStatus.values()) : out;
    }

    private LocalDate parseDate(String s) {
        try { return LocalDate.parse(s.trim()); }
        catch (RuntimeException e) { throw ApiException.badRequest("Date must look like 2026-10-31."); }
    }
}
