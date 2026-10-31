package com.befit.backend.service;

import com.befit.backend.dto.PatientHistoryDTO;
import com.befit.backend.dto.PatientProfileDTO;
import com.befit.backend.entity.Appointment;
import com.befit.backend.entity.AppointmentStatus;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.Role;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** What each kind of staff member actually receives from GET /api/patients/{id} and /{id}/history. */
class PatientServiceCrossBranchProfileTest {
    private AppointmentRepository appointments;
    private BillRepository bills;
    private PatientBranchHistoryRepository history;
    private PatientRepository patients;
    private BranchRepository branches;
    private PatientService service;
    private Patient patient;

    @BeforeEach
    void setUp() {
        patients = mock(PatientRepository.class);
        appointments = mock(AppointmentRepository.class);
        bills = mock(BillRepository.class);
        history = mock(PatientBranchHistoryRepository.class);
        branches = mock(BranchRepository.class);
        BranchService branchService = mock(BranchService.class);
        TransferService transfers = mock(TransferService.class);
        when(branchService.nameMap()).thenReturn(Map.of("KOTHRUD", "Kothrud", "BANER", "Baner", "WAKAD", "Wakad"));
        BranchAccess access = new BranchAccess(branches, appointments, bills, history, 3);
        service = new PatientService(patients, history, appointments, bills, branches, branchService, transfers, access, mock(PatientInvitationRepository.class));

        patient = new Patient();
        patient.setId(7L);
        patient.setFullName("Asha Kulkarni");
        patient.setPhone("+91 98765 43210");
        patient.setGender("Female");
        patient.setDateOfBirth(LocalDate.of(1990, 4, 2));
        patient.setAddress("12 Hill Road, Pune");
        patient.setEmergencyContact("9000000001");
        patient.setEmergencyContactName("Ravi");
        patient.setBloodGroup("O+");
        patient.setOccupation("Teacher");
        patient.setMedicalHistory("Lumbar disc herniation, 2021");
        patient.setBranchId("KOTHRUD");
        when(patients.findById(7L)).thenReturn(Optional.of(patient));
        when(branches.findAllIds()).thenReturn(List.of("KOTHRUD", "BANER", "WAKAD"));
    }

    private static AuthUser user(Role role, String branch) { return new AuthUser(1L, "u@befit.test", "U", role, branch); }

    private void liveVisitAt(String branch) {
        when(appointments.existsLiveVisit(eq(7L), eq(branch), anyCollection(), eq(AppointmentStatus.COMPLETED), any(LocalDateTime.class)))
                .thenReturn(true);
    }

    @Test void homeBranchStaffSeeTheFullProfile() {
        PatientProfileDTO dto = service.getForStaff(user(Role.RECEPTIONIST, "KOTHRUD"), 7L);
        assertEquals("FULL", dto.accessLevel());
        assertEquals("+91 98765 43210", dto.phone());
        assertEquals("12 Hill Road, Pune", dto.address());
        assertEquals("Lumbar disc herniation, 2021", dto.medicalHistory());
        assertEquals("Ravi", dto.emergencyContactName());
    }

    @Test void adminSeesTheFullProfile() {
        assertEquals("FULL", service.getForStaff(user(Role.ADMIN, null), 7L).accessLevel());
    }

    @Test void receivingBranchSeesOnlyIdentification() {
        liveVisitAt("BANER");
        PatientProfileDTO dto = service.getForStaff(user(Role.RECEPTIONIST, "BANER"), 7L);
        assertEquals("TEMPORARY_VISIT", dto.accessLevel());
        assertEquals("REG-7", dto.regId());
        assertEquals("Asha Kulkarni", dto.fullName());
        assertEquals("Female", dto.gender());
        assertEquals(LocalDate.of(1990, 4, 2), dto.dateOfBirth());
        assertEquals("******3210", dto.phone());
        assertEquals("Kothrud", dto.branchName());
    }

    @Test void limitedProfileNeverCarriesSensitiveFields() {
        liveVisitAt("BANER");
        PatientProfileDTO dto = service.getForStaff(user(Role.DOCTOR, "BANER"), 7L);
        assertNull(dto.address());
        assertNull(dto.emergencyContact());
        assertNull(dto.emergencyContactName());
        assertNull(dto.bloodGroup());
        assertNull(dto.occupation());
        assertNull(dto.medicalHistory());
        assertNull(dto.registrationDate());
        assertNull(dto.pendingTransfer());
    }

    @Test void formerBranchGetsTheSameLimitedProfile() {
        when(appointments.existsByPatientIdAndBranchId(7L, "WAKAD")).thenReturn(true);
        PatientProfileDTO dto = service.getForStaff(user(Role.RECEPTIONIST, "WAKAD"), 7L);
        assertEquals("HISTORICAL", dto.accessLevel());
        assertNull(dto.medicalHistory());
        assertNull(dto.address());
    }

    @Test void unrelatedBranchIsRejectedWith403() {
        ApiException e = assertThrows(ApiException.class, () -> service.getForStaff(user(Role.RECEPTIONIST, "BANER"), 7L));
        assertEquals(403, e.getStatus());
        e = assertThrows(ApiException.class, () -> service.historyForStaff(user(Role.DOCTOR, "BANER"), 7L));
        assertEquals(403, e.getStatus());
    }

    @Test void unknownPatientIs404() {
        ApiException e = assertThrows(ApiException.class, () -> service.getForStaff(user(Role.ADMIN, null), 999L));
        assertEquals(404, e.getStatus());
    }

    @Test void limitedHistoryIsOwnBranchOnlyWithNoBranchMoveLog() {
        liveVisitAt("BANER");
        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setBranchId("BANER");
        a.setStatus(AppointmentStatus.WAITING);
        when(appointments.findByPatientIdAndBranchIdInOrderByAppointmentDateDesc(eq(7L), eq(List.of("BANER")), any())).thenReturn(List.of(a));

        PatientHistoryDTO h = service.historyForStaff(user(Role.DOCTOR, "BANER"), 7L);
        assertEquals(1, h.appointments().size());
        assertTrue(h.bills().isEmpty(), "doctors never get invoices in a limited view");
        assertTrue(h.branchHistory().isEmpty());
        // The query itself was restricted to the caller's branch - other branches' visits are never even loaded.
        verify(appointments).findByPatientIdAndBranchIdInOrderByAppointmentDateDesc(eq(7L), eq(List.of("BANER")), any());
        verify(appointments, never()).findByPatientIdOrderByAppointmentDateDesc(any(), any());
    }

    @Test void limitedHistoryForReceptionistIncludesOwnBranchInvoicesButNoBranchMoves() {
        liveVisitAt("BANER");
        service.historyForStaff(user(Role.RECEPTIONIST, "BANER"), 7L);
        verify(bills).findByPatientIdAndBranchIdInOrderByBillDateDesc(eq(7L), eq(List.of("BANER")), any());
        verify(history, never()).findByPatientIdAndBranchIdInOrderByFromDateDesc(eq(7L), anyCollection());
    }

    @Test void homeBranchHistoryStillIncludesBranchMoves() {
        service.historyForStaff(user(Role.RECEPTIONIST, "KOTHRUD"), 7L);
        verify(history).findByPatientIdAndBranchIdInOrderByFromDateDesc(eq(7L), eq(List.of("KOTHRUD")));
    }

    @Test void crossBranchSearchOnlyMarksViewableWhenAProfileCanBeOpened() {
        liveVisitAt("BANER");
        when(patients.findById(7L)).thenReturn(Optional.of(patient));
        var rows = service.searchAcrossBranches(user(Role.RECEPTIONIST, "BANER"), "REG-7", null, null, null);
        assertTrue(rows.get(0).viewable());
        var other = service.searchAcrossBranches(user(Role.RECEPTIONIST, "WAKAD"), "REG-7", null, null, null);
        assertFalse(other.get(0).viewable());
    }
}
