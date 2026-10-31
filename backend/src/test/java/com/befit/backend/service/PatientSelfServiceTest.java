package com.befit.backend.service;

import com.befit.backend.dto.*;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** BEFIT-31 patient portal: a patient reaches only their own record, from any branch, before and after a transfer. */
class PatientSelfServiceTest {
    private PatientRepository patients;
    private AppointmentRepository appointments;
    private BillRepository bills;
    private PatientBranchHistoryRepository history;
    private BranchRepository branches;
    private BranchTransferRequestRepository transfers;
    private BranchService branchService;
    private TransferService transferService;
    private PatientService service;
    private Patient asha;
    private Patient ravi;

    private static final AuthUser ASHA = new AuthUser(10L, "asha@mail.test", "Asha", Role.PATIENT, "A");

    @BeforeEach
    void setUp() {
        patients = mock(PatientRepository.class);
        appointments = mock(AppointmentRepository.class);
        bills = mock(BillRepository.class);
        history = mock(PatientBranchHistoryRepository.class);
        branches = mock(BranchRepository.class);
        transfers = mock(BranchTransferRequestRepository.class);
        branchService = mock(BranchService.class);
        BranchAccess access = new BranchAccess(branches, appointments, bills, history, 3);
        transferService = new TransferService(patients, history, transfers, appointments, bills, branchService, access, true, false, false, true);
        service = new PatientService(patients, history, appointments, bills, branches, branchService, transferService, access, mock(PatientInvitationRepository.class));

        asha = patient(7L, "Asha Kulkarni", "A");
        ravi = patient(8L, "Ravi Patil", "A");
        when(patients.findByUserEmail("asha@mail.test")).thenReturn(Optional.of(asha));
        when(patients.findByUserEmail("ravi@mail.test")).thenReturn(Optional.of(ravi));
        when(patients.findById(7L)).thenReturn(Optional.of(asha));
        when(patients.findById(8L)).thenReturn(Optional.of(ravi));
        when(patients.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));
        when(branches.findAllIds()).thenReturn(List.of("A", "B", "C"));
        when(branchService.nameMap()).thenReturn(Map.of("A", "Branch A", "B", "Branch B", "C", "Branch C"));
        when(branchService.require("A")).thenReturn(new Branch("A", "Branch A"));
        when(branchService.require("B")).thenReturn(new Branch("B", "Branch B"));
        when(transfers.save(any(BranchTransferRequest.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Patient patient(long id, String name, String branch) {
        Patient p = new Patient();
        p.setId(id); p.setFullName(name); p.setBranchId(branch); p.setPhone("9820000000"); p.setMedicalHistory("private notes");
        return p;
    }

    private static Appointment appointment(long id, Patient p, String branch, AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setId(id); a.setPatient(p); a.setBranchId(branch); a.setStatus(status);
        a.setAppointmentDate(LocalDateTime.now().minusDays(id)); a.setDoctorName("Dr X"); a.setCaseType("Consultation");
        return a;
    }

    // ------------------------------------------------------------ own profile, identity only
    @Test void patientGetsTheirOwnProfileFoundByTheirLoginIdentity() {
        PatientProfileDTO dto = service.myProfile(ASHA);
        assertEquals(7L, dto.id());
        assertEquals("Asha Kulkarni", dto.fullName());
        assertEquals("SELF", dto.accessLevel());
        assertEquals("private notes", dto.medicalHistory());            // their own data is complete
        verify(patients).findByUserEmail("asha@mail.test");
        verify(patients, never()).findById(any());                     // never looked up by an id the caller could choose
    }

    @Test void twoPatientsAtTheSameBranchGetOnlyTheirOwnRecords() {
        assertEquals(7L, service.myProfile(ASHA).id());
        assertEquals(8L, service.myProfile(new AuthUser(11L, "ravi@mail.test", "Ravi", Role.PATIENT, "A")).id());
    }

    @Test void patientWithoutALinkedProfileGets404() {
        assertEquals(404, assertThrows(ApiException.class, () -> service.myProfile(new AuthUser(12L, "ghost@mail.test", "G", Role.PATIENT, null))).getStatus());
    }

    @Test void staffTokensCannotUseThePatientSelfServiceApis() {
        for (Role r : List.of(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST, Role.BRANCH_MANAGER)) {
            var staff = new AuthUser(1L, "asha@mail.test", "S", r, "A");      // even with a matching e-mail
            assertEquals(403, assertThrows(ApiException.class, () -> service.myProfile(staff)).getStatus(), r.name());
            assertEquals(403, assertThrows(ApiException.class, () -> service.myHistory(staff)).getStatus(), r.name());
            assertEquals(403, assertThrows(ApiException.class, () -> service.updateMyProfile(staff, new PatientProfileUpdate())).getStatus(), r.name());
            assertEquals(403, assertThrows(ApiException.class, () -> transferService.myTransfers(staff)).getStatus(), r.name());
            assertEquals(403, assertThrows(ApiException.class, () -> transferService.requestTransfer(staff, new TransferRequestBody("B", "x", null))).getStatus(), r.name());
            assertEquals(403, assertThrows(ApiException.class, () -> transferService.cancelMine(staff)).getStatus(), r.name());
        }
        verify(patients, never()).save(any());
        verify(transfers, never()).save(any());
    }

    // ------------------------------------------------------------ cannot reach another patient
    @Test void patientCannotOpenAnotherPatientsFileThroughTheStaffApis() {
        var asha = ASHA;                                    // even asking for her own id through the staff API: not a patient endpoint
        assertEquals(403, assertThrows(ApiException.class, () -> service.getForStaff(asha, 8L)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.historyForStaff(asha, 8L)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> service.getForStaff(asha, 7L)).getStatus());
    }

    @Test void updateChangesOnlyTheCallersOwnPatientAndNeverTheBranch() {
        PatientProfileUpdate u = new PatientProfileUpdate();
        u.setFullName("  Asha K  "); u.setPhone("9811111111"); u.setGender("female");
        PatientProfileDTO dto = service.updateMyProfile(ASHA, u);

        assertEquals("Asha K", asha.getFullName());
        assertEquals("9811111111", asha.getPhone());
        assertEquals("A", asha.getBranchId(), "the branch is only ever changed by a transfer");
        assertEquals("A", dto.branchId());
        assertEquals("Ravi Patil", ravi.getFullName(), "another patient is untouched");
        verify(patients).save(asha);
        verify(patients, never()).save(ravi);
    }

    // ------------------------------------------------------------ not tied to a branch, and survives a transfer
    @Test void historyCoversEveryBranchNotTheBranchOfTheCaller() {
        service.myHistory(ASHA);
        verify(appointments).findByPatientIdAndBranchIdInOrderByAppointmentDateDesc(eq(7L), eq(List.of("A", "B", "C")), any());
        verify(bills).findByPatientIdAndBranchIdInOrderByBillDateDesc(eq(7L), eq(List.of("A", "B", "C")), any());
    }

    @Test void afterAnApprovedTransferThePatientSeesTheNewBranchAndTheOldVisits() {
        Appointment oldVisit = appointment(1, asha, "A", AppointmentStatus.COMPLETED);
        Appointment newVisit = appointment(2, asha, "B", AppointmentStatus.WAITING);
        when(appointments.findByPatientIdAndBranchIdInOrderByAppointmentDateDesc(eq(7L), anyCollection(), any())).thenReturn(List.of(newVisit, oldVisit));

        // the destination branch manager approves the pending request
        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(7L); t.setFromBranchId("A"); t.setToBranchId("B"); t.setRequestedBy("asha@mail.test");
        when(transfers.findById(5L)).thenReturn(Optional.of(t));
        transferService.approve(new AuthUser(2L, "mgr@B.test", "M", Role.BRANCH_MANAGER, "B"), 5L, null, false);

        PatientProfileDTO profile = service.myProfile(ASHA);            // same login, no branch filter in sight
        assertEquals("B", profile.branchId());
        assertEquals("Branch B", profile.branchName());
        PatientHistoryDTO h = service.myHistory(ASHA);
        assertEquals(List.of("B", "A"), h.appointments().stream().map(AppointmentDTO::branchId).toList());   // old visit still at A
        assertEquals("A", oldVisit.getBranchId());
    }

    @Test void branchMovesShownToThePatientCarryNoStaffEmail() {
        PatientBranchHistory stint = new PatientBranchHistory(7L, "A");
        stint.setAuthorizedBy("mgr@B.test"); stint.setReason("Moved house");
        when(history.findByPatientIdAndBranchIdInOrderByFromDateDesc(eq(7L), anyCollection())).thenReturn(List.of(stint));
        var stints = service.myHistory(ASHA).branchHistory();
        assertEquals(1, stints.size());
        assertNull(stints.get(0).authorizedBy());
        assertEquals("Moved house", stints.get(0).reason());
    }

    @Test void patientSeesTheirInvoicesFromEveryBranch() {
        Bill b = new Bill();
        b.setId(1L); b.setInvoiceNumber("INV-1"); b.setPatientId(7L); b.setBranchId("A"); b.setStatus(BillStatus.PARTIAL);
        b.setBillDate(LocalDateTime.now()); b.setTotalAmount(new BigDecimal("500.00")); b.setAmountPaid(new BigDecimal("100.00"));
        when(bills.findByPatientIdAndBranchIdInOrderByBillDateDesc(eq(7L), anyCollection(), any())).thenReturn(List.of(b));
        assertEquals("A", service.myHistory(ASHA).bills().get(0).branchId());
    }

    // ------------------------------------------------------------ transfers a patient may start and track
    @Test void patientRequestsATransferOfTheirOwnRecordOnly() {
        TransferDTO dto = transferService.requestTransfer(ASHA, new TransferRequestBody("B", "Moved house", null));
        assertEquals("PENDING", dto.status());
        assertEquals(7L, dto.patientId());
        assertEquals("A", dto.fromBranchId());
        assertEquals("A", asha.getBranchId(), "a request does not move the patient");
        assertEquals("Moved house", dto.reason());
    }

    @Test void patientTransferRulesAreEnforced() {
        assertEquals(400, assertThrows(ApiException.class, () -> transferService.requestTransfer(ASHA, new TransferRequestBody("A", "same", null))).getStatus());
        Branch off = new Branch("B", "Branch B"); off.setActive(false);
        when(branchService.require("B")).thenReturn(off);
        assertEquals(400, assertThrows(ApiException.class, () -> transferService.requestTransfer(ASHA, new TransferRequestBody("B", "x", null))).getStatus());
        when(branchService.require("B")).thenReturn(new Branch("B", "Branch B"));
        when(transfers.findFirstByPatientIdAndStatus(7L, TransferStatus.PENDING)).thenReturn(Optional.of(new BranchTransferRequest()));
        assertEquals("TRANSFER_PENDING", assertThrows(ApiException.class, () -> transferService.requestTransfer(ASHA, new TransferRequestBody("B", "x", null))).getCode());
    }

    @Test void aPatientRequestAlwaysWaitsForAnApproverWhenMoneyIsDue() {
        Bill b = new Bill();
        b.setId(1L); b.setInvoiceNumber("INV-1"); b.setPatientId(7L); b.setBranchId("A"); b.setStatus(BillStatus.UNPAID);
        b.setBillDate(LocalDateTime.now()); b.setTotalAmount(new BigDecimal("500.00")); b.setAmountPaid(BigDecimal.ZERO);
        when(bills.findByPatientIdAndStatusInOrderByBillDateAsc(eq(7L), anyCollection())).thenReturn(List.of(b));
        assertEquals("PENDING", transferService.requestTransfer(ASHA, new TransferRequestBody("B", "x", null)).status());
        assertEquals("A", asha.getBranchId());
    }

    @Test void patientTracksOnlyTheirOwnRequestsWithoutStaffOrMoneyDetails() {
        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(7L); t.setFromBranchId("A"); t.setToBranchId("B"); t.setStatus(TransferStatus.APPROVED);
        t.setDecidedBy("mgr@B.test"); t.setDecidedAt(LocalDateTime.now()); t.setDecisionNote("Welcome");
        t.setDecisionWarnings("1 invoice due"); t.setOutstandingAmount(new BigDecimal("400.00")); t.setOutstandingInvoices(1);
        when(transfers.findByPatientIdOrderByRequestedAtDesc(eq(7L), any())).thenReturn(List.of(t));

        List<TransferDTO> mine = transferService.myTransfers(ASHA);
        assertEquals(1, mine.size());
        TransferDTO d = mine.get(0);
        assertEquals("APPROVED", d.status());
        assertEquals("Welcome", d.decisionNote());
        assertNotNull(d.decidedAt());
        assertNull(d.decidedBy());
        assertNull(d.decisionWarnings());
        assertNull(d.outstandingAmount());
        verify(transfers).findByPatientIdOrderByRequestedAtDesc(eq(7L), any());
        verify(transfers, never()).findByPatientIdOrderByRequestedAtDesc(eq(8L), any());
    }

    @Test void patientCanCancelOnlyTheirOwnPendingRequest() {
        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(7L); t.setFromBranchId("A"); t.setToBranchId("B");
        when(transfers.findFirstByPatientIdAndStatus(7L, TransferStatus.PENDING)).thenReturn(Optional.of(t));
        assertEquals("CANCELLED", transferService.cancelMine(ASHA).status());
        verify(transfers, never()).findFirstByPatientIdAndStatus(eq(8L), any());
        // Ravi has nothing pending: he cannot cancel Asha's request, he just gets "nothing to cancel"
        assertEquals(404, assertThrows(ApiException.class, () -> transferService.cancelMine(new AuthUser(11L, "ravi@mail.test", "Ravi", Role.PATIENT, "A"))).getStatus());
    }

    @Test void patientsCannotDecideTransfers() {
        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(7L); t.setFromBranchId("A"); t.setToBranchId("B");
        when(transfers.findById(5L)).thenReturn(Optional.of(t));
        assertEquals(403, assertThrows(ApiException.class, () -> transferService.approve(ASHA, 5L, null, false)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> transferService.reject(ASHA, 5L, "x")).getStatus());
        assertEquals("A", asha.getBranchId());
    }
}
