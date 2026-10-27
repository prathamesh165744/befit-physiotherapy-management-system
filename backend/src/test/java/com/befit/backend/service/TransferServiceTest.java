package com.befit.backend.service;

import com.befit.backend.dto.StaffTransferRequest;
import com.befit.backend.dto.TransferDTO;
import com.befit.backend.dto.TransferRequestBody;
import com.befit.backend.dto.TransferValidationDTO;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Permanent transfer workflow (BEFIT-26): who can raise/decide, what is validated, what changes and what must not. */
class TransferServiceTest {
    private PatientRepository patients;
    private PatientBranchHistoryRepository history;
    private BranchTransferRequestRepository transfers;
    private AppointmentRepository appointments;
    private BillRepository bills;
    private BranchService branchService;
    private TransferService service;
    private Patient patient;

    @BeforeEach
    void setUp() { build(false, false); }

    private void build(boolean blockOnInvoices, boolean allowSelfApproval) {
        patients = mock(PatientRepository.class);
        history = mock(PatientBranchHistoryRepository.class);
        transfers = mock(BranchTransferRequestRepository.class);
        appointments = mock(AppointmentRepository.class);
        bills = mock(BillRepository.class);
        branchService = mock(BranchService.class);
        BranchAccess access = new BranchAccess(mock(BranchRepository.class), appointments, bills, history, 3);
        service = new TransferService(patients, history, transfers, appointments, bills, branchService, access, true, blockOnInvoices, allowSelfApproval);

        patient = new Patient();
        patient.setId(7L); patient.setFullName("Asha Kulkarni"); patient.setBranchId("A");
        when(patients.findById(7L)).thenReturn(Optional.of(patient));
        when(patients.findAllById(any())).thenReturn(List.of(patient));
        when(branchService.require("A")).thenReturn(new Branch("A", "Branch A"));
        when(branchService.require("B")).thenReturn(new Branch("B", "Branch B"));
        when(branchService.nameMap()).thenReturn(Map.of("A", "Branch A", "B", "Branch B", "C", "Branch C"));
        when(transfers.save(any(BranchTransferRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(bills.outstandingBalance(eq(7L), anyCollection())).thenReturn(BigDecimal.ZERO);
    }

    private static AuthUser staff(Role role, String branch, String email) { return new AuthUser(1L, email, "S", role, branch); }
    private static AuthUser staff(Role role, String branch) { return staff(role, branch, role.name().toLowerCase() + "@" + branch + ".test"); }

    private BranchTransferRequest pending(String requestedBy) {
        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(7L); t.setFromBranchId("A"); t.setToBranchId("B"); t.setReason("Moved house"); t.setRequestedBy(requestedBy);
        when(transfers.findById(5L)).thenReturn(Optional.of(t));
        return t;
    }

    // ----------------------------------------------------------------- create
    @Test void sourceBranchReceptionistRaisesPendingRequest() {
        TransferDTO dto = service.createByStaff(staff(Role.RECEPTIONIST, "A"), new StaffTransferRequest(7L, "A", "B", "Moved house"));
        assertEquals("PENDING", dto.status());
        assertEquals("A", dto.fromBranchId());
        assertEquals("B", dto.toBranchId());
        assertEquals("Moved house", dto.reason());
        assertEquals("RECEPTIONIST", dto.requestedByRole());
        assertEquals("A", patient.getBranchId(), "creating a request must not move the patient");
        verify(history, never()).save(any());
    }

    @Test void destinationBranchReceptionistAndAdminMayAlsoRaiseIt() {
        assertEquals("PENDING", service.createByStaff(staff(Role.RECEPTIONIST, "B"), new StaffTransferRequest(7L, null, "B", "x")).status());
        assertEquals("PENDING", service.createByStaff(staff(Role.ADMIN, null), new StaffTransferRequest(7L, null, "B", "x")).status());
    }

    @Test void unrelatedBranchDoctorAndPatientCannotRaiseIt() {
        var body = new StaffTransferRequest(7L, null, "B", "x");
        assertEquals(403, assertThrows(ApiException.class, () -> service.createByStaff(staff(Role.RECEPTIONIST, "C"), body)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.createByStaff(staff(Role.DOCTOR, "A"), body)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.createByStaff(staff(Role.PATIENT, "A"), body)).getStatus());
        verify(transfers, never()).save(any());
    }

    @Test void sourceAndDestinationMustBeValidAndDifferent() {
        var same = new StaffTransferRequest(7L, null, "A", "x");
        assertEquals(400, assertThrows(ApiException.class, () -> service.createByStaff(staff(Role.RECEPTIONIST, "A"), same)).getStatus());
        Branch off = new Branch("B", "Branch B"); off.setActive(false);
        when(branchService.require("B")).thenReturn(off);
        assertEquals(400, assertThrows(ApiException.class, () -> service.createByStaff(staff(Role.ADMIN, null), new StaffTransferRequest(7L, null, "B", "x"))).getStatus());
        when(branchService.require("Z")).thenThrow(ApiException.notFound("Branch not found."));
        assertEquals(404, assertThrows(ApiException.class, () -> service.createByStaff(staff(Role.ADMIN, null), new StaffTransferRequest(7L, null, "Z", "x"))).getStatus());
    }

    @Test void staleSourceAndDuplicatePendingAreRejected() {
        assertEquals("TRANSFER_STALE", assertThrows(ApiException.class,
                () -> service.createByStaff(staff(Role.ADMIN, null), new StaffTransferRequest(7L, "C", "B", "x"))).getCode());
        when(transfers.findFirstByPatientIdAndStatus(7L, TransferStatus.PENDING)).thenReturn(Optional.of(new BranchTransferRequest()));
        assertEquals("TRANSFER_PENDING", assertThrows(ApiException.class,
                () -> service.createByStaff(staff(Role.ADMIN, null), new StaffTransferRequest(7L, null, "B", "x"))).getCode());
    }

    // ----------------------------------------------------------------- validation
    @Test void cleanPatientHasNoBlockersOrWarnings() {
        pending("a@A.test");
        TransferValidationDTO v = service.validationFor(staff(Role.RECEPTIONIST, "B"), 5L);
        assertTrue(v.canApprove());
        assertFalse(v.requiresAcknowledgement());
        assertTrue(v.blockers().isEmpty() && v.warnings().isEmpty());
    }

    @Test void openAppointmentTodayOrOverdueBlocks() {
        pending("a@A.test");
        when(appointments.countByPatientIdAndStatusInAndAppointmentDateBefore(eq(7L), anyCollection(), any())).thenReturn(1L);
        TransferValidationDTO v = service.validationFor(staff(Role.RECEPTIONIST, "B"), 5L);
        assertFalse(v.canApprove());
        assertEquals("OPEN_APPOINTMENT", v.blockers().get(0).code());
    }

    @Test void futureAppointmentsAndOutstandingInvoicesAreWarnings() {
        pending("a@A.test");
        when(appointments.countByPatientIdAndStatusInAndAppointmentDateGreaterThanEqual(eq(7L), anyCollection(), any())).thenReturn(2L);
        when(bills.countByPatientIdAndStatusIn(eq(7L), anyCollection())).thenReturn(1L);
        when(bills.outstandingBalance(eq(7L), anyCollection())).thenReturn(new BigDecimal("1250"));
        TransferValidationDTO v = service.validationFor(staff(Role.RECEPTIONIST, "B"), 5L);
        assertTrue(v.canApprove());
        assertTrue(v.requiresAcknowledgement());
        assertEquals(List.of("FUTURE_APPOINTMENT", "OUTSTANDING_INVOICES"), v.warnings().stream().map(i -> i.code()).toList());
        assertEquals(new BigDecimal("1250"), v.outstandingAmount());
    }

    @Test void outstandingInvoicesBlockWhenTheClinicRuleSaysSo() {
        build(true, false);
        pending("a@A.test");
        when(bills.countByPatientIdAndStatusIn(eq(7L), anyCollection())).thenReturn(1L);
        when(bills.outstandingBalance(eq(7L), anyCollection())).thenReturn(new BigDecimal("300"));
        TransferValidationDTO v = service.validationFor(staff(Role.RECEPTIONIST, "B"), 5L);
        assertFalse(v.canApprove());
        assertEquals("OUTSTANDING_INVOICES", v.blockers().get(0).code());
    }

    @Test void unrelatedBranchCannotSeeTheValidation() {
        pending("a@A.test");
        assertEquals(403, assertThrows(ApiException.class, () -> service.validationFor(staff(Role.RECEPTIONIST, "C"), 5L)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.validationFor(staff(Role.DOCTOR, "B"), 5L)).getStatus());
    }

    // ----------------------------------------------------------------- approve
    @Test void approveMovesPatientRecordsHistoryAndAuditFields() {
        BranchTransferRequest t = pending("a@A.test");
        TransferDTO dto = service.approve(staff(Role.RECEPTIONIST, "B", "r@B.test"), 5L, "Welcome", false);

        assertEquals("APPROVED", dto.status());
        assertEquals("B", patient.getBranchId());
        assertEquals("r@B.test", t.getDecidedBy());
        assertNotNull(t.getDecidedAt());
        assertEquals("Welcome", t.getDecisionNote());
        assertEquals("a@A.test", t.getRequestedBy());
        assertEquals("Moved house", t.getReason());
        verify(history).save(argThat((PatientBranchHistory h) -> "B".equals(h.getBranchId()) && "r@B.test".equals(h.getAuthorizedBy())));
        verify(patients).save(patient);
        // history of the old branch is untouched: no appointment or bill is saved/updated by a transfer
        verify(appointments, never()).save(any());
        verify(bills, never()).save(any());
    }

    @Test void approveWithWarningsNeedsAcknowledgementAndNote() {
        pending("a@A.test");
        when(appointments.countByPatientIdAndStatusInAndAppointmentDateGreaterThanEqual(eq(7L), anyCollection(), any())).thenReturn(1L);
        var r = staff(Role.RECEPTIONIST, "B", "r@B.test");
        assertEquals("ACKNOWLEDGEMENT_REQUIRED", assertThrows(ApiException.class, () -> service.approve(r, 5L, "ok", false)).getCode());
        assertEquals(400, assertThrows(ApiException.class, () -> service.approve(r, 5L, "  ", true)).getStatus());
        assertEquals("A", patient.getBranchId());

        TransferDTO dto = service.approve(r, 5L, "Patient will rebook", true);
        assertEquals("B", patient.getBranchId());
        assertTrue(dto.decisionWarnings().contains("upcoming appointment"), "acknowledged warnings are stored for the audit trail");
    }

    @Test void approveIsBlockedByOpenAppointmentEvenIfAcknowledged() {
        pending("a@A.test");
        when(appointments.countByPatientIdAndStatusInAndAppointmentDateBefore(eq(7L), anyCollection(), any())).thenReturn(1L);
        ApiException e = assertThrows(ApiException.class, () -> service.approve(staff(Role.RECEPTIONIST, "B"), 5L, "go", true));
        assertEquals("TRANSFER_BLOCKED", e.getCode());
        assertEquals("A", patient.getBranchId());
        verify(history, never()).save(any());
    }

    @Test void approveIsBlockedWhenPatientBranchChangedMeanwhile() {
        pending("a@A.test");
        patient.setBranchId("C");
        assertEquals("TRANSFER_BLOCKED", assertThrows(ApiException.class, () -> service.approve(staff(Role.ADMIN, null), 5L, null, false)).getCode());
    }

    @Test void onlyDestinationReceptionistOrAdminMayApprove() {
        pending("a@A.test");
        assertEquals(403, assertThrows(ApiException.class, () -> service.approve(staff(Role.RECEPTIONIST, "A"), 5L, null, false)).getStatus()); // source
        assertEquals(403, assertThrows(ApiException.class, () -> service.approve(staff(Role.RECEPTIONIST, "C"), 5L, null, false)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.approve(staff(Role.DOCTOR, "B"), 5L, null, false)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.approve(staff(Role.PATIENT, "B"), 5L, null, false)).getStatus());
        assertEquals("A", patient.getBranchId());
        assertEquals("APPROVED", service.approve(staff(Role.ADMIN, null), 5L, null, false).status());
    }

    @Test void requesterCannotApproveTheirOwnRequestUnlessAllowed() {
        pending("r@B.test");
        assertEquals(403, assertThrows(ApiException.class, () -> service.approve(staff(Role.RECEPTIONIST, "B", "r@B.test"), 5L, null, false)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.reject(staff(Role.RECEPTIONIST, "B", "R@b.test"), 5L, "no")).getStatus());
        build(false, true);
        pending("r@B.test");
        assertEquals("APPROVED", service.approve(staff(Role.RECEPTIONIST, "B", "r@B.test"), 5L, null, false).status());
    }

    @Test void decidedRequestsCannotBeDecidedAgain() {
        BranchTransferRequest t = pending("a@A.test");
        t.setStatus(TransferStatus.REJECTED);
        assertEquals("TRANSFER_DECIDED", assertThrows(ApiException.class, () -> service.approve(staff(Role.ADMIN, null), 5L, null, false)).getCode());
        assertEquals("TRANSFER_DECIDED", assertThrows(ApiException.class, () -> service.cancel(staff(Role.ADMIN, null), 5L, null)).getCode());
    }

    // ----------------------------------------------------------------- reject / cancel
    @Test void rejectKeepsThePatientWhereTheyAreAndRecordsTheDecision() {
        BranchTransferRequest t = pending("a@A.test");
        TransferDTO dto = service.reject(staff(Role.RECEPTIONIST, "B", "r@B.test"), 5L, "No capacity");
        assertEquals("REJECTED", dto.status());
        assertEquals("A", patient.getBranchId());
        assertEquals("r@B.test", t.getDecidedBy());
        assertEquals("No capacity", t.getDecisionNote());
        assertNotNull(t.getDecidedAt());
        verify(history, never()).save(any());
        verify(patients, never()).save(any());
    }

    @Test void rejectByOtherBranchIsForbidden() {
        pending("a@A.test");
        assertEquals(403, assertThrows(ApiException.class, () -> service.reject(staff(Role.RECEPTIONIST, "C"), 5L, "x")).getStatus());
    }

    @Test void sourceOrDestinationStaffCanCancelButOthersCannot() {
        BranchTransferRequest t = pending("a@A.test");
        assertEquals(403, assertThrows(ApiException.class, () -> service.cancel(staff(Role.RECEPTIONIST, "C"), 5L, null)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.cancel(staff(Role.DOCTOR, "A"), 5L, null)).getStatus());
        assertEquals("CANCELLED", service.cancel(staff(Role.RECEPTIONIST, "A"), 5L, "Raised by mistake").status());
        assertEquals("A", patient.getBranchId());
        assertEquals("Raised by mistake", t.getDecisionNote());
    }

    // ----------------------------------------------------------------- admin direct
    @Test void adminDirectTransferAppliesRulesToo() {
        var body = new TransferRequestBody("B", "Relocation", false);
        assertEquals(403, assertThrows(ApiException.class, () -> service.adminTransfer(staff(Role.RECEPTIONIST, "A"), 7L, body)).getStatus());

        when(appointments.countByPatientIdAndStatusInAndAppointmentDateGreaterThanEqual(eq(7L), anyCollection(), any())).thenReturn(1L);
        assertEquals("ACKNOWLEDGEMENT_REQUIRED", assertThrows(ApiException.class, () -> service.adminTransfer(staff(Role.ADMIN, null), 7L, body)).getCode());
        assertEquals("A", patient.getBranchId());

        TransferDTO dto = service.adminTransfer(staff(Role.ADMIN, null), 7L, new TransferRequestBody("B", "Relocation", true));
        assertEquals("APPROVED", dto.status());
        assertEquals("B", patient.getBranchId());
        assertNotNull(dto.decisionWarnings());
    }

    @Test void adminDirectTransferIsBlockedByOpenAppointment() {
        when(appointments.countByPatientIdAndStatusInAndAppointmentDateBefore(eq(7L), anyCollection(), any())).thenReturn(1L);
        assertEquals("TRANSFER_BLOCKED", assertThrows(ApiException.class,
                () -> service.adminTransfer(staff(Role.ADMIN, null), 7L, new TransferRequestBody("B", "x", true))).getCode());
        assertEquals("A", patient.getBranchId());
    }

    // ----------------------------------------------------------------- lists
    @Test void listsAreScopedToTheCallersBranchAndDirection() {
        service.listForStaff(staff(Role.RECEPTIONIST, "B"), null, "PENDING", "INCOMING");
        verify(transfers).findByToBranchIdInAndStatusOrderByRequestedAtDesc(eq(List.of("B")), eq(TransferStatus.PENDING), any());
        service.listForStaff(staff(Role.RECEPTIONIST, "A"), null, "ALL", "OUTGOING");
        verify(transfers).findByFromBranchIdInOrderByRequestedAtDesc(eq(List.of("A")), any());
        assertEquals(403, assertThrows(ApiException.class, () -> service.listForStaff(staff(Role.DOCTOR, "A"), null, "ALL", "OUTGOING")).getStatus());
    }
}
