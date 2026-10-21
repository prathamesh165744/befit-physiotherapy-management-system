package com.befit.backend.security;

import com.befit.backend.entity.AppointmentStatus;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.PatientBranchHistory;
import com.befit.backend.entity.Role;
import com.befit.backend.repository.AppointmentRepository;
import com.befit.backend.repository.BillRepository;
import com.befit.backend.repository.BranchRepository;
import com.befit.backend.repository.PatientBranchHistoryRepository;
import com.befit.backend.security.BranchAccess.PatientAccessLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Who may open a patient's profile, and how much of it - for every role / branch relationship. */
class BranchAccessPatientAccessTest {
    private AppointmentRepository appointments;
    private BillRepository bills;
    private PatientBranchHistoryRepository history;
    private BranchAccess access;
    private Patient patient;

    @BeforeEach
    void setUp() {
        appointments = mock(AppointmentRepository.class);
        bills = mock(BillRepository.class);
        history = mock(PatientBranchHistoryRepository.class);
        access = new BranchAccess(mock(BranchRepository.class), appointments, bills, history, 3);
        patient = new Patient();
        patient.setId(7L);
        patient.setBranchId("KOTHRUD");   // the patient's home branch
    }

    private static AuthUser user(Role role, String branch) { return new AuthUser(1L, "u@befit.test", "U", role, branch); }

    private void liveVisitAt(String branch) {
        when(appointments.existsLiveVisit(eq(7L), eq(branch), anyCollection(), eq(AppointmentStatus.COMPLETED), any(LocalDateTime.class)))
                .thenReturn(true);
    }

    @Test void adminHasFullAccess() {
        assertEquals(PatientAccessLevel.FULL, access.patientAccess(user(Role.ADMIN, null), patient));
    }

    @Test void homeBranchReceptionistAndDoctorHaveFullAccess() {
        assertEquals(PatientAccessLevel.FULL, access.patientAccess(user(Role.RECEPTIONIST, "KOTHRUD"), patient));
        assertEquals(PatientAccessLevel.FULL, access.patientAccess(user(Role.DOCTOR, "KOTHRUD"), patient));
    }

    @Test void receivingBranchWithLiveVisitGetsTemporaryVisitAccess() {
        liveVisitAt("BANER");
        assertEquals(PatientAccessLevel.TEMPORARY_VISIT, access.patientAccess(user(Role.RECEPTIONIST, "BANER"), patient));
        assertEquals(PatientAccessLevel.TEMPORARY_VISIT, access.patientAccess(user(Role.DOCTOR, "BANER"), patient));
    }

    @Test void liveVisitAtAnotherBranchDoesNotHelpThisBranch() {
        liveVisitAt("BANER");
        assertEquals(PatientAccessLevel.DENIED, access.patientAccess(user(Role.RECEPTIONIST, "WAKAD"), patient));
    }

    @Test void formerHomeBranchGetsHistoricalAccess() {
        when(history.findByPatientIdAndBranchIdInOrderByFromDateDesc(eq(7L), eq(List.of("BANER"))))
                .thenReturn(List.of(new PatientBranchHistory(7L, "BANER")));
        assertEquals(PatientAccessLevel.HISTORICAL, access.patientAccess(user(Role.RECEPTIONIST, "BANER"), patient));
    }

    @Test void branchThatOnlyTreatedOrBilledThePatientGetsHistoricalAccess() {
        when(appointments.existsByPatientIdAndBranchId(7L, "BANER")).thenReturn(true);
        assertEquals(PatientAccessLevel.HISTORICAL, access.patientAccess(user(Role.DOCTOR, "BANER"), patient));
        when(bills.existsByPatientIdAndBranchId(7L, "WAKAD")).thenReturn(true);
        assertEquals(PatientAccessLevel.HISTORICAL, access.patientAccess(user(Role.RECEPTIONIST, "WAKAD"), patient));
    }

    @Test void liveVisitWinsOverHistorical() {
        liveVisitAt("BANER");
        when(appointments.existsByPatientIdAndBranchId(7L, "BANER")).thenReturn(true);
        assertEquals(PatientAccessLevel.TEMPORARY_VISIT, access.patientAccess(user(Role.RECEPTIONIST, "BANER"), patient));
    }

    @Test void unrelatedBranchIsDenied() {
        assertEquals(PatientAccessLevel.DENIED, access.patientAccess(user(Role.RECEPTIONIST, "BANER"), patient));
        assertEquals(PatientAccessLevel.DENIED, access.patientAccess(user(Role.DOCTOR, "BANER"), patient));
    }

    @Test void staffWithoutABranchIsDenied() {
        assertEquals(PatientAccessLevel.DENIED, access.patientAccess(user(Role.RECEPTIONIST, null), patient));
        assertEquals(PatientAccessLevel.DENIED, access.patientAccess(user(Role.DOCTOR, " "), patient));
    }

    @Test void patientRoleNeverGetsStaffAccessEvenAtTheSameBranch() {
        assertEquals(PatientAccessLevel.DENIED, access.patientAccess(user(Role.PATIENT, "KOTHRUD"), patient));
    }

    @Test void onlyLimitedLevelsAreLimited() {
        assertEquals(List.of(false, true, true, false),
                List.of(PatientAccessLevel.FULL.isLimited(), PatientAccessLevel.TEMPORARY_VISIT.isLimited(),
                        PatientAccessLevel.HISTORICAL.isLimited(), PatientAccessLevel.DENIED.isLimited()));
    }
}
