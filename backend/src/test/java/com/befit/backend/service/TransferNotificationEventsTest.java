package com.befit.backend.service;

import com.befit.backend.dto.StaffTransferRequest;
import com.befit.backend.dto.TransferRequestBody;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** BEFIT-33: every transfer action raises exactly the right notification event - and a refused action raises none. */
class TransferNotificationEventsTest {
    private PatientRepository patients;
    private BranchTransferRequestRepository transfers;
    private NotificationService notifications;
    private TransferService service;
    private Patient patient;

    private static AuthUser staff(Role role, String branch, String email) { return new AuthUser(1L, email, "S", role, branch); }

    @BeforeEach
    void setUp() {
        patients = mock(PatientRepository.class);
        transfers = mock(BranchTransferRequestRepository.class);
        notifications = mock(NotificationService.class);
        BranchService branchService = mock(BranchService.class);
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        BillRepository bills = mock(BillRepository.class);
        PatientBranchHistoryRepository history = mock(PatientBranchHistoryRepository.class);
        BranchAccess access = new BranchAccess(mock(BranchRepository.class), appointments, bills, history, 3);
        service = new TransferService(patients, history, transfers, appointments, bills, branchService, access, true, false, false, true, notifications);

        patient = new Patient();
        patient.setId(7L); patient.setFullName("Asha"); patient.setBranchId("A");
        when(patients.findById(7L)).thenReturn(Optional.of(patient));
        when(branchService.require("A")).thenReturn(new Branch("A", "Branch A"));
        when(branchService.require("B")).thenReturn(new Branch("B", "Branch B"));
        when(branchService.nameMap()).thenReturn(Map.of("A", "Branch A", "B", "Branch B"));
        when(transfers.save(any(BranchTransferRequest.class))).thenAnswer(i -> i.getArgument(0));
    }

    private BranchTransferRequest pending(String requestedBy) {
        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(7L); t.setFromBranchId("A"); t.setToBranchId("B"); t.setRequestedBy(requestedBy);
        when(transfers.findById(5L)).thenReturn(Optional.of(t));
        return t;
    }

    @Test void aStaffRequestRaisesTheRequestedEvent() {
        var actor = staff(Role.RECEPTIONIST, "A", "rec@A.test");
        service.createByStaff(actor, new StaffTransferRequest(7L, null, "B", "Moved house"));
        verify(notifications).transferRequested(any(BranchTransferRequest.class), eq(patient), eq(actor));
        verify(notifications, never()).transferDecided(any(), any(), any());
    }

    @Test void aPatientRequestRaisesTheRequestedEvent() {
        patient.setUser(new User());
        when(patients.findByUserEmail("asha@mail.test")).thenReturn(Optional.of(patient));
        var patientUser = new AuthUser(9L, "asha@mail.test", "Asha", Role.PATIENT, "A");
        service.requestTransfer(patientUser, new TransferRequestBody("B", "x", null));
        verify(notifications).transferRequested(any(), eq(patient), eq(patientUser));
    }

    @Test void approvingRaisesTheDecidedEventWithTheApprovedStatus() {
        BranchTransferRequest t = pending("rec@A.test");
        var mgr = staff(Role.BRANCH_MANAGER, "B", "mgr@B.test");
        service.approve(mgr, 5L, null, false);
        verify(notifications).transferDecided(argThat((BranchTransferRequest x) -> x.getStatus() == TransferStatus.APPROVED), eq(patient), eq(mgr));
        assertEquals(TransferStatus.APPROVED, t.getStatus());
    }

    @Test void rejectingRaisesTheDecidedEventWithTheRejectedStatus() {
        pending("rec@A.test");
        var mgr = staff(Role.BRANCH_MANAGER, "B", "mgr@B.test");
        service.reject(mgr, 5L, "No capacity");
        verify(notifications).transferDecided(argThat((BranchTransferRequest x) -> x.getStatus() == TransferStatus.REJECTED), eq(patient), eq(mgr));
    }

    @Test void withdrawingRaisesTheCancelledEvent() {
        pending("rec@A.test");
        var rec = staff(Role.RECEPTIONIST, "A", "rec@A.test");
        service.cancel(rec, 5L, "mistake");
        verify(notifications).transferCancelled(any(), eq(patient), eq(rec));
    }

    @Test void aPatientCancellingRaisesTheCancelledEvent() {
        patient.setUser(new User());
        when(patients.findByUserEmail("asha@mail.test")).thenReturn(Optional.of(patient));
        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(7L); t.setFromBranchId("A"); t.setToBranchId("B");
        when(transfers.findFirstByPatientIdAndStatus(7L, TransferStatus.PENDING)).thenReturn(Optional.of(t));
        var patientUser = new AuthUser(9L, "asha@mail.test", "Asha", Role.PATIENT, "A");
        service.cancelMine(patientUser);
        verify(notifications).transferCancelled(any(), eq(patient), eq(patientUser));
    }

    @Test void anAdminDirectTransferIsReportedAsADecision() {
        var admin = staff(Role.ADMIN, null, "admin@x.test");
        service.adminTransfer(admin, 7L, new TransferRequestBody("B", "Relocation", true));
        verify(notifications).transferDecided(argThat((BranchTransferRequest x) -> x.getStatus() == TransferStatus.APPROVED), eq(patient), eq(admin));
        verify(notifications, never()).transferRequested(any(), any(), any());
    }

    @Test void refusedActionsRaiseNoNotification() {
        pending("rec@A.test");
        assertThrows(ApiException.class, () -> service.approve(staff(Role.RECEPTIONIST, "B", "r@B.test"), 5L, null, false));   // receptionists cannot decide
        assertThrows(ApiException.class, () -> service.reject(staff(Role.BRANCH_MANAGER, "C", "m@C.test"), 5L, "x"));          // wrong branch
        assertThrows(ApiException.class, () -> service.cancel(staff(Role.RECEPTIONIST, "C", "r@C.test"), 5L, null));
        assertThrows(ApiException.class, () -> service.createByStaff(staff(Role.RECEPTIONIST, "C", "r@C.test"), new StaffTransferRequest(7L, null, "B", "x")));
        verifyNoInteractions(notifications);
    }
}
