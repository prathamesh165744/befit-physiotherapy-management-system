package com.befit.backend.service;

import com.befit.backend.dto.TransferDTO;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** BEFIT-30: the source branch tracks its outgoing requests - and only those - without entering the destination's queue. */
class TransferOutgoingTrackingTest {
    private PatientRepository patients;
    private BranchTransferRequestRepository transfers;
    private BranchRepository branches;
    private TransferService service;

    @BeforeEach
    void setUp() {
        patients = mock(PatientRepository.class);
        transfers = mock(BranchTransferRequestRepository.class);
        branches = mock(BranchRepository.class);
        BranchService branchService = mock(BranchService.class);
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        BillRepository bills = mock(BillRepository.class);
        PatientBranchHistoryRepository history = mock(PatientBranchHistoryRepository.class);
        BranchAccess access = new BranchAccess(branches, appointments, bills, history, 3);
        service = new TransferService(patients, history, transfers, appointments, bills, branchService, access, true, false, false, true);

        Patient p = new Patient();
        p.setId(7L); p.setFullName("Asha Kulkarni"); p.setBranchId("A");
        when(patients.findAllById(any())).thenReturn(List.of(p));
        when(branches.findAllIds()).thenReturn(List.of("A", "B", "C"));
        when(branchService.nameMap()).thenReturn(Map.of("A", "Branch A", "B", "Branch B", "C", "Branch C"));
    }

    private static AuthUser staff(Role role, String branch) { return new AuthUser(1L, role.name().toLowerCase() + "@" + branch + ".test", "S", role, branch); }

    private static BranchTransferRequest request(long id, TransferStatus status) {
        BranchTransferRequest t = new BranchTransferRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(t, "id", id);
        t.setPatientId(7L); t.setFromBranchId("A"); t.setToBranchId("B");
        t.setReason("Moved house"); t.setRequestedBy("rec@A.test"); t.setRequestedByRole("RECEPTIONIST");
        t.setRequestedAt(LocalDateTime.of(2026, 10, 20, 9, 30));
        t.setStatus(status);
        if (status != TransferStatus.PENDING) {
            t.setDecidedBy("mgr@B.test"); t.setDecidedAt(LocalDateTime.of(2026, 10, 21, 11, 0)); t.setDecisionNote("Note " + status);
            // what only the destination approver should see:
            t.setDecisionWarnings("2 unpaid invoice(s), 1250.00 still due");
            t.setOutstandingAmount(new BigDecimal("1250.00")); t.setOutstandingInvoices(2); t.setOutstandingInvoiceRefs("INV-1@A, INV-2@A");
        }
        return t;
    }

    // ------------------------------------------------------------ what the source branch sees
    @Test void sourceStaffSeeTheirOutgoingRequestsWithPatientBranchesReasonStatusAndDecision() {
        when(transfers.findByFromBranchIdInOrderByRequestedAtDesc(eq(List.of("A")), any()))
                .thenReturn(List.of(request(1, TransferStatus.PENDING), request(2, TransferStatus.APPROVED),
                                    request(3, TransferStatus.REJECTED), request(4, TransferStatus.CANCELLED)));
        List<TransferDTO> rows = service.listForStaff(staff(Role.RECEPTIONIST, "A"), null, "ALL", "OUTGOING");

        assertEquals(List.of("PENDING", "APPROVED", "REJECTED", "CANCELLED"), rows.stream().map(TransferDTO::status).toList());
        TransferDTO pending = rows.get(0);
        assertEquals(7L, pending.patientId());
        assertEquals("Asha Kulkarni", pending.patientName());
        assertEquals("Branch A", pending.fromBranchName());
        assertEquals("Branch B", pending.toBranchName());
        assertEquals("Moved house", pending.reason());
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 30), pending.requestedAt());
        assertNull(pending.decidedBy(), "nobody has decided a pending request yet");
        assertNull(pending.decidedAt());

        for (TransferDTO decided : rows.subList(1, 4)) {
            assertEquals("mgr@B.test", decided.decidedBy());                       // approver / decider
            assertEquals(LocalDateTime.of(2026, 10, 21, 11, 0), decided.decidedAt());
            assertEquals("Note " + decided.status(), decided.decisionNote());       // decision details
        }
    }

    @Test void theDestinationApproversInternalReviewDetailsAreNotSentToTheSourceBranch() {
        when(transfers.findByFromBranchIdInOrderByRequestedAtDesc(eq(List.of("A")), any())).thenReturn(List.of(request(2, TransferStatus.APPROVED)));
        TransferDTO dto = service.listForStaff(staff(Role.RECEPTIONIST, "A"), null, "ALL", "OUTGOING").get(0);
        assertNull(dto.decisionWarnings());
        assertNull(dto.outstandingAmount());
        assertNull(dto.outstandingInvoices());
        assertNull(dto.outstandingInvoiceRefs());
        assertEquals("APPROVED", dto.status());
    }

    @Test void statusFilterIsAppliedToOutgoingRequests() {
        for (String s : List.of("PENDING", "APPROVED", "REJECTED", "CANCELLED")) {
            service.listForStaff(staff(Role.BRANCH_MANAGER, "A"), null, s, "OUTGOING");
            verify(transfers).findByFromBranchIdInAndStatusOrderByRequestedAtDesc(eq(List.of("A")), eq(TransferStatus.valueOf(s)), any());
        }
        assertEquals(400, assertThrows(ApiException.class, () -> service.listForStaff(staff(Role.RECEPTIONIST, "A"), null, "SHIPPED", "OUTGOING")).getStatus());
    }

    // ------------------------------------------------------------ branch restriction
    @Test void staffOnlyEverQueryTheirOwnBranchWhateverTheyAskFor() {
        var rec = staff(Role.RECEPTIONIST, "A");
        service.listForStaff(rec, "B", "ALL", "OUTGOING");           // asking for branch B's outgoing list
        service.listForStaff(rec, "ALL", "ALL", "OUTGOING");
        verify(transfers, times(2)).findByFromBranchIdInOrderByRequestedAtDesc(eq(List.of("A")), any());
        verify(transfers, never()).findByFromBranchIdInOrderByRequestedAtDesc(eq(List.of("B")), any());
    }

    @Test void anotherBranchSeesNothingOfBranchAsOutgoingRequests() {
        service.listForStaff(staff(Role.RECEPTIONIST, "C"), null, "ALL", "OUTGOING");
        verify(transfers).findByFromBranchIdInOrderByRequestedAtDesc(eq(List.of("C")), any());
        verify(transfers, never()).findByFromBranchIdInOrderByRequestedAtDesc(eq(List.of("A")), any());
    }

    @Test void sourceStaffCannotReachTheDestinationsApprovalQueue() {
        // the INCOMING list of a source-branch user is THEIR OWN incoming queue, never branch B's
        service.listForStaff(staff(Role.RECEPTIONIST, "A"), "B", "PENDING", "INCOMING");
        verify(transfers).findByToBranchIdInAndStatusOrderByRequestedAtDesc(eq(List.of("A")), eq(TransferStatus.PENDING), any());
        verify(transfers, never()).findByToBranchIdInAndStatusOrderByRequestedAtDesc(eq(List.of("B")), any(), any());

        // ...and the approval review of a request into B is not theirs to open or decide
        BranchTransferRequest t = request(5, TransferStatus.PENDING);
        when(transfers.findById(5L)).thenReturn(Optional.of(t));
        var source = staff(Role.RECEPTIONIST, "A");
        assertEquals(403, assertThrows(ApiException.class, () -> service.validationFor(source, 5L)).getStatus());
        var sourceManager = staff(Role.BRANCH_MANAGER, "A");
        assertEquals(403, assertThrows(ApiException.class, () -> service.validationFor(sourceManager, 5L)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.approve(sourceManager, 5L, null, false)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.reject(sourceManager, 5L, "x")).getStatus());
        assertEquals(TransferStatus.PENDING, t.getStatus());
    }

    @Test void adminSeesEveryBranchsOutgoingRequests() {
        service.listForStaff(staff(Role.ADMIN, null), null, "ALL", "OUTGOING");
        verify(transfers).findByFromBranchIdInOrderByRequestedAtDesc(eq(List.of("A", "B", "C")), any());
    }

    @Test void doctorsAndPatientsCannotUseTheTrackingApis() {
        for (Role r : List.of(Role.DOCTOR, Role.PATIENT)) {
            var u = staff(r, "A");
            assertEquals(403, assertThrows(ApiException.class, () -> service.listForStaff(u, null, "ALL", "OUTGOING")).getStatus());
            assertEquals(403, assertThrows(ApiException.class, () -> service.summaryForStaff(u, null, "OUTGOING")).getStatus());
        }
        verifyNoInteractions(transfers);
    }

    // ------------------------------------------------------------ status counts
    @Test void summaryCountsEveryStatusAndFillsMissingOnesWithZero() {
        when(transfers.countOutgoingByStatus(eq(List.of("A"))))
                .thenReturn(List.of(new Object[]{TransferStatus.PENDING, 3L}, new Object[]{TransferStatus.REJECTED, 1L}));
        Map<String, Long> s = service.summaryForStaff(staff(Role.RECEPTIONIST, "A"), "B", "OUTGOING");
        assertEquals(Map.of("PENDING", 3L, "APPROVED", 0L, "REJECTED", 1L, "CANCELLED", 0L), s);
        verify(transfers, never()).countOutgoingByStatus(eq(List.of("B")));
    }

    @Test void incomingSummaryIsAlsoScopedToTheCallersBranch() {
        service.summaryForStaff(staff(Role.BRANCH_MANAGER, "B"), "A", "INCOMING");
        verify(transfers).countIncomingByStatus(eq(List.of("B")));
        verify(transfers, never()).countOutgoingByStatus(any());
    }

    // ------------------------------------------------------------ source staff keep their own actions
    @Test void sourceStaffCanStillWithdrawTheirPendingRequestButNotOthers() {
        BranchTransferRequest t = request(5, TransferStatus.PENDING);
        when(transfers.findById(5L)).thenReturn(Optional.of(t));
        when(transfers.save(any(BranchTransferRequest.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals(403, assertThrows(ApiException.class, () -> service.cancel(staff(Role.RECEPTIONIST, "C"), 5L, null)).getStatus());
        assertEquals("CANCELLED", service.cancel(staff(Role.RECEPTIONIST, "A"), 5L, "Patient changed mind").status());
    }
}
