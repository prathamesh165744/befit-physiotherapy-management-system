package com.befit.backend.service;

import com.befit.backend.dto.StaffTransferRequest;
import com.befit.backend.dto.TransferDTO;
import com.befit.backend.dto.TransferPermissionsDTO;
import com.befit.backend.dto.TransferRequestBody;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.security.BranchAccess.PatientAccessLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** BEFIT-29: who may approve/reject a permanent transfer once manager approval is required, and how far a manager reaches. */
class TransferManagerApprovalTest {
    private PatientRepository patients;
    private PatientBranchHistoryRepository history;
    private BranchTransferRequestRepository transfers;
    private AppointmentRepository appointments;
    private BillRepository bills;
    private BranchService branchService;
    private BranchRepository branches;
    private BranchAccess access;
    private TransferService service;
    private Patient patient;

    @BeforeEach
    void setUp() { build(true); }

    private void build(boolean managerApprovalRequired) {
        patients = mock(PatientRepository.class);
        history = mock(PatientBranchHistoryRepository.class);
        transfers = mock(BranchTransferRequestRepository.class);
        appointments = mock(AppointmentRepository.class);
        bills = mock(BillRepository.class);
        branchService = mock(BranchService.class);
        branches = mock(BranchRepository.class);
        access = new BranchAccess(branches, appointments, bills, history, 3);
        service = new TransferService(patients, history, transfers, appointments, bills, branchService, access,
                true, false, false, managerApprovalRequired);

        patient = new Patient();
        patient.setId(7L); patient.setFullName("Asha Kulkarni"); patient.setBranchId("A");
        when(patients.findById(7L)).thenReturn(Optional.of(patient));
        when(patients.findAllById(any())).thenReturn(List.of(patient));
        when(branches.findAllIds()).thenReturn(List.of("A", "B", "C"));
        when(branchService.require("A")).thenReturn(new Branch("A", "Branch A"));
        when(branchService.require("B")).thenReturn(new Branch("B", "Branch B"));
        when(branchService.nameMap()).thenReturn(Map.of("A", "Branch A", "B", "Branch B", "C", "Branch C"));
        when(transfers.save(any(BranchTransferRequest.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static AuthUser user(Role role, String branch, String email) { return new AuthUser(1L, email, "U", role, branch); }
    private static AuthUser user(Role role, String branch) { return user(role, branch, role.name().toLowerCase() + "@" + branch + ".test"); }

    private BranchTransferRequest pending(String requestedBy) {
        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(7L); t.setFromBranchId("A"); t.setToBranchId("B"); t.setReason("Moved house"); t.setRequestedBy(requestedBy);
        when(transfers.findById(5L)).thenReturn(Optional.of(t));
        return t;
    }

    // ------------------------------------------------------------- receptionists: raise yes, decide no
    @Test void receptionistsCanRaiseAndWithdrawButNotApproveOrReject() {
        var rA = user(Role.RECEPTIONIST, "A");
        assertEquals("PENDING", service.createByStaff(rA, new StaffTransferRequest(7L, "A", "B", "Moved house")).status());
        assertEquals("PENDING", service.createByStaff(user(Role.RECEPTIONIST, "B"), new StaffTransferRequest(7L, null, "B", "x")).status());

        BranchTransferRequest t = pending("requester@A.test");
        var rB = user(Role.RECEPTIONIST, "B");            // receptionist of the DESTINATION branch
        ApiException approve = assertThrows(ApiException.class, () -> service.approve(rB, 5L, "ok", false));
        ApiException reject = assertThrows(ApiException.class, () -> service.reject(rB, 5L, "no"));
        assertEquals(403, approve.getStatus());
        assertEquals(403, reject.getStatus());
        assertEquals(TransferStatus.PENDING, t.getStatus());
        assertEquals("A", patient.getBranchId());
        verify(history, never()).save(any());

        assertEquals("CANCELLED", service.cancel(rA, 5L, "Raised by mistake").status());   // withdrawing is still allowed
    }

    // ------------------------------------------------------------- managers decide, for their own branch only
    @Test void destinationManagerApprovesAndTheDecisionIsRecorded() {
        BranchTransferRequest t = pending("requester@A.test");
        TransferDTO dto = service.approve(user(Role.BRANCH_MANAGER, "B", "mgr@B.test"), 5L, "Welcome", false);
        assertEquals("APPROVED", dto.status());
        assertEquals("B", patient.getBranchId());
        assertEquals("mgr@B.test", t.getDecidedBy());
        assertNotNull(t.getDecidedAt());
        verify(history).save(argThat((PatientBranchHistory h) -> "B".equals(h.getBranchId()) && "mgr@B.test".equals(h.getAuthorizedBy())));
    }

    @Test void destinationManagerRejectsWithoutMovingThePatient() {
        BranchTransferRequest t = pending("requester@A.test");
        assertEquals("REJECTED", service.reject(user(Role.BRANCH_MANAGER, "B", "mgr@B.test"), 5L, "No capacity").status());
        assertEquals("A", patient.getBranchId());
        assertEquals("mgr@B.test", t.getDecidedBy());
        verify(patients, never()).save(any());
    }

    @Test void adminStillApproves() {
        pending("requester@A.test");
        assertEquals("APPROVED", service.approve(user(Role.ADMIN, null), 5L, null, false).status());
    }

    @Test void managersOfOtherBranchesAndOfTheSourceBranchCannotDecide() {
        pending("requester@A.test");
        for (String branch : List.of("A", "C")) {                          // source branch, and an unrelated branch
            var m = user(Role.BRANCH_MANAGER, branch);
            assertEquals(403, assertThrows(ApiException.class, () -> service.approve(m, 5L, null, false)).getStatus());
            assertEquals(403, assertThrows(ApiException.class, () -> service.reject(m, 5L, "x")).getStatus());
        }
        assertEquals("A", patient.getBranchId());
    }

    @Test void managerWithoutABranchHasNoAccessAtAll() {
        pending("requester@A.test");
        for (String none : new String[]{null, " "}) {
            var m = user(Role.BRANCH_MANAGER, none);
            assertEquals(403, assertThrows(ApiException.class, () -> service.approve(m, 5L, null, false)).getStatus());
            assertEquals(403, assertThrows(ApiException.class, () -> service.validationFor(m, 5L)).getStatus());
            assertEquals(403, assertThrows(ApiException.class, () -> service.createByStaff(m, new StaffTransferRequest(7L, null, "B", "x"))).getStatus());
        }
        assertFalse(service.permissionsFor(user(Role.BRANCH_MANAGER, null)).canDecide());
    }

    @Test void managerCannotApproveARequestTheyRaisedThemselves() {
        pending("mgr@B.test");
        assertEquals(403, assertThrows(ApiException.class, () -> service.approve(user(Role.BRANCH_MANAGER, "B", "mgr@B.test"), 5L, null, false)).getStatus());
    }

    @Test void doctorsAndPatientsCanNeverDecide() {
        pending("requester@A.test");
        for (Role r : List.of(Role.DOCTOR, Role.PATIENT)) {
            var u = user(r, "B");
            assertEquals(403, assertThrows(ApiException.class, () -> service.approve(u, 5L, null, false)).getStatus());
            assertEquals(403, assertThrows(ApiException.class, () -> service.reject(u, 5L, "x")).getStatus());
        }
    }

    @Test void decidedRequestsStayDecidedEvenForManagers() {
        BranchTransferRequest t = pending("requester@A.test");
        t.setStatus(TransferStatus.APPROVED);
        assertEquals("TRANSFER_DECIDED", assertThrows(ApiException.class, () -> service.reject(user(Role.BRANCH_MANAGER, "B"), 5L, "x")).getCode());
    }

    // ------------------------------------------------------------- the clinic rule can be switched off
    @Test void withoutManagerApprovalTheDestinationReceptionistMayDecideAgain() {
        build(false);
        pending("requester@A.test");
        assertEquals("APPROVED", service.approve(user(Role.RECEPTIONIST, "B"), 5L, null, false).status());

        build(false);
        pending("requester@A.test");
        assertEquals("REJECTED", service.reject(user(Role.RECEPTIONIST, "B"), 5L, "x").status());

        // ...but only the DESTINATION branch's receptionist, and never a doctor
        build(false);
        pending("requester@A.test");
        assertEquals(403, assertThrows(ApiException.class, () -> service.approve(user(Role.RECEPTIONIST, "C"), 5L, null, false)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.approve(user(Role.DOCTOR, "B"), 5L, null, false)).getStatus());

        // managers keep working either way
        build(false);
        pending("requester@A.test");
        assertEquals("APPROVED", service.approve(user(Role.BRANCH_MANAGER, "B"), 5L, null, false).status());
    }

    // ------------------------------------------------------------- a manager is not an admin
    @Test void managerCannotMoveAPatientDirectly() {
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.adminTransfer(user(Role.BRANCH_MANAGER, "B"), 7L, new TransferRequestBody("B", "x", true))).getStatus());
        assertEquals("A", patient.getBranchId());
    }

    @Test void managerCanRaiseOnlyForTheirOwnBranch() {
        assertEquals("PENDING", service.createByStaff(user(Role.BRANCH_MANAGER, "A"), new StaffTransferRequest(7L, null, "B", "x")).status());   // source
        assertEquals("PENDING", service.createByStaff(user(Role.BRANCH_MANAGER, "B"), new StaffTransferRequest(7L, null, "B", "x")).status());   // destination
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.createByStaff(user(Role.BRANCH_MANAGER, "C"), new StaffTransferRequest(7L, null, "B", "x"))).getStatus());
    }

    @Test void managerListsAreScopedToTheirOwnBranchWhatEverIsRequested() {
        var m = user(Role.BRANCH_MANAGER, "B");
        service.listForStaff(m, "A", "PENDING", "INCOMING");     // asking for branch A is ignored
        verify(transfers).findByToBranchIdInAndStatusOrderByRequestedAtDesc(eq(List.of("B")), eq(TransferStatus.PENDING), any());
        service.listForStaff(m, "ALL", "ALL", "OUTGOING");
        verify(transfers).findByFromBranchIdInOrderByRequestedAtDesc(eq(List.of("B")), any());
        verify(transfers, never()).findByToBranchIdInAndStatusOrderByRequestedAtDesc(eq(List.of("A", "B", "C")), any(), any());
    }

    @Test void managerSeesTheValidationOnlyOfTransfersTouchingTheirBranch() {
        pending("requester@A.test");
        assertTrue(service.validationFor(user(Role.BRANCH_MANAGER, "B"), 5L).canApprove());
        assertEquals(403, assertThrows(ApiException.class, () -> service.validationFor(user(Role.BRANCH_MANAGER, "C"), 5L)).getStatus());
    }

    @Test void managerCanWithdrawOnlyRequestsOfTheirOwnBranch() {
        pending("requester@A.test");
        assertEquals(403, assertThrows(ApiException.class, () -> service.cancel(user(Role.BRANCH_MANAGER, "C"), 5L, null)).getStatus());
        assertEquals("CANCELLED", service.cancel(user(Role.BRANCH_MANAGER, "B"), 5L, null).status());
    }

    @Test void managerRoleGivesNoAdminWideBranchAccess() {
        var m = user(Role.BRANCH_MANAGER, "B");
        assertEquals(List.of("B"), access.visibleBranchIds(m, null));
        assertEquals(List.of("B"), access.visibleBranchIds(m, "A"));
        assertEquals(List.of("B"), access.visibleBranchIds(m, "ALL"));
        assertEquals("B", access.branchForCreate(m, "A"));
        assertEquals(403, assertThrows(ApiException.class, () -> access.requireBranch(m, "A")).getStatus());
        assertFalse(m.isAdmin());
        assertTrue(m.isBranchManager());
    }

    @Test void managerGetsNoPatientProfileAccessAndCannotBookVisits() {
        var m = user(Role.BRANCH_MANAGER, "A");                               // even at the patient's own branch
        assertEquals(PatientAccessLevel.DENIED, access.patientAccess(m, patient));
        assertEquals(403, assertThrows(ApiException.class, () -> access.branchForVisit(user(Role.BRANCH_MANAGER, "B"), patient, "B")).getStatus());
    }

    // ------------------------------------------------------------- what the UI is told
    @Test void permissionsMatchTheRoleAndTheClinicRule() {
        TransferPermissionsDTO rec = service.permissionsFor(user(Role.RECEPTIONIST, "B"));
        assertTrue(rec.canCreate());
        assertFalse(rec.canDecide());
        assertTrue(rec.managerApprovalRequired());
        assertTrue(service.permissionsFor(user(Role.BRANCH_MANAGER, "B")).canDecide());
        assertTrue(service.permissionsFor(user(Role.ADMIN, null)).canDecide());
        TransferPermissionsDTO doctor = service.permissionsFor(user(Role.DOCTOR, "B"));
        assertFalse(doctor.canCreate());
        assertFalse(doctor.canDecide());
        assertFalse(service.permissionsFor(user(Role.PATIENT, "B")).canCreate());

        build(false);
        assertTrue(service.permissionsFor(user(Role.RECEPTIONIST, "B")).canDecide());
        assertFalse(service.permissionsFor(user(Role.RECEPTIONIST, "B")).managerApprovalRequired());
    }

    @Test void decisionRoleRuleIsExplicit() {
        assertTrue(BranchAccess.mayDecideTransfers(user(Role.ADMIN, null), true));
        assertTrue(BranchAccess.mayDecideTransfers(user(Role.BRANCH_MANAGER, "B"), true));
        assertFalse(BranchAccess.mayDecideTransfers(user(Role.RECEPTIONIST, "B"), true));
        assertTrue(BranchAccess.mayDecideTransfers(user(Role.RECEPTIONIST, "B"), false));
        assertFalse(BranchAccess.mayDecideTransfers(user(Role.DOCTOR, "B"), false));
        assertFalse(BranchAccess.mayDecideTransfers(user(Role.PATIENT, "B"), false));
    }
}
