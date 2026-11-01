package com.befit.backend.service;

import com.befit.backend.dto.NotificationDTO;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.BranchTransferRequestRepository;
import com.befit.backend.repository.NotificationRepository;
import com.befit.backend.repository.PatientRepository;
import com.befit.backend.repository.UserRepository;
import com.befit.backend.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** BEFIT-33: who is notified, what they are told, what they can open, and that nobody reaches another user's notifications. */
class NotificationServiceTest {
    private NotificationRepository notifications;
    private UserRepository users;
    private PatientRepository patients;
    private BranchTransferRequestRepository transfers;
    private NotificationService service;
    private Patient patient;
    private BranchTransferRequest transfer;
    private final List<Notification> saved = new ArrayList<>();

    private static User user(long id, Role role, String branch, String email, String name) {
        User u = new User();
        u.setId(id); u.setRole(role); u.setBranchId(branch); u.setEmail(email); u.setFullName(name); u.setActive(true);
        return u;
    }
    private static AuthUser auth(User u) { return new AuthUser(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getBranchId()); }

    private User admin, mgrB, mgrA, recB, recA, patientLogin;

    @BeforeEach
    void setUp() { build(true); }

    private void build(boolean managerApprovalRequired) {
        saved.clear();
        notifications = mock(NotificationRepository.class);
        users = mock(UserRepository.class);
        patients = mock(PatientRepository.class);
        transfers = mock(BranchTransferRequestRepository.class);
        BranchService branchService = mock(BranchService.class);
        when(branchService.nameMap()).thenReturn(Map.of("A", "Branch A", "B", "Branch B"));
        service = new NotificationService(notifications, users, patients, transfers, branchService, mock(PlatformTransactionManager.class), managerApprovalRequired);

        admin = user(1, Role.ADMIN, null, "admin@x.test", "Admin");
        mgrB = user(2, Role.BRANCH_MANAGER, "B", "mgr@B.test", "Meera (B)");
        mgrA = user(3, Role.BRANCH_MANAGER, "A", "mgr@A.test", "Manoj (A)");
        recB = user(4, Role.RECEPTIONIST, "B", "rec@B.test", "Rita (B)");
        recA = user(5, Role.RECEPTIONIST, "A", "rec@A.test", "Ravi (A)");
        patientLogin = user(6, Role.PATIENT, null, "asha@mail.test", "Asha Kulkarni");

        patient = new Patient();
        patient.setId(7L); patient.setFullName("Asha Kulkarni"); patient.setBranchId("A"); patient.setUser(patientLogin);
        transfer = new BranchTransferRequest();
        ReflectionTestUtils.setField(transfer, "id", 5L);
        transfer.setPatientId(7L); transfer.setFromBranchId("A"); transfer.setToBranchId("B"); transfer.setRequestedBy("rec@A.test");

        when(users.findByRoleAndActiveTrue(Role.ADMIN)).thenReturn(List.of(admin));
        when(users.findByRoleAndBranchIdAndActiveTrue(Role.BRANCH_MANAGER, "B")).thenReturn(List.of(mgrB));
        when(users.findByRoleAndBranchIdAndActiveTrue(Role.BRANCH_MANAGER, "A")).thenReturn(List.of(mgrA));
        when(users.findByRoleAndBranchIdAndActiveTrue(Role.RECEPTIONIST, "B")).thenReturn(List.of(recB));
        when(users.findByEmail("rec@A.test")).thenReturn(Optional.of(recA));
        when(notifications.save(any(Notification.class))).thenAnswer(i -> { saved.add(i.getArgument(0)); return i.getArgument(0); });
    }

    private Set<Long> recipients() { Set<Long> s = new TreeSet<>(); saved.forEach(n -> s.add(n.getRecipientUserId())); return s; }
    private Notification to(long userId) { return saved.stream().filter(n -> n.getRecipientUserId() == userId).findFirst().orElseThrow(); }

    // ------------------------------------------------------------ who gets what
    @Test void aTransferRequestNotifiesTheDecidersOnly() {
        service.transferRequested(transfer, patient, auth(recA));
        assertEquals(Set.of(1L, 2L), recipients(), "admin + destination manager - not the source branch, not receptionists, not the patient");
        Notification n = to(2);
        assertEquals(NotificationType.TRANSFER_REQUESTED, n.getType());
        assertEquals("TRANSFER", n.getRelatedType());
        assertEquals(5L, n.getRelatedId());
        assertTrue(n.getMessage().contains("Asha Kulkarni (REG-7)"));
        assertTrue(n.getMessage().contains("Branch A") && n.getMessage().contains("Branch B"));
        assertFalse(n.isRead());
    }

    @Test void destinationReceptionistsAreToldOnlyWhenTheyMayDecide() {
        service.transferRequested(transfer, patient, auth(recA));
        assertFalse(recipients().contains(4L));
        build(false);
        service.transferRequested(transfer, patient, auth(recA));
        assertTrue(recipients().contains(4L));
    }

    @Test void thePersonWhoActedIsNeverNotified() {
        service.transferRequested(transfer, patient, auth(mgrB));       // a destination manager raised it themself
        assertEquals(Set.of(1L), recipients());
        saved.clear();
        service.transferRequested(transfer, patient, auth(admin));
        assertEquals(Set.of(2L), recipients());
    }

    @Test void inactiveAccountsAreSkipped() {
        mgrB.setActive(false);
        service.transferRequested(transfer, patient, auth(recA));
        assertEquals(Set.of(1L), recipients());
    }

    @Test void anApprovalTellsTheRequesterThePatientAndTheSourceManagers() {
        transfer.setStatus(TransferStatus.APPROVED);
        service.transferDecided(transfer, patient, auth(mgrB));
        assertEquals(Set.of(3L, 5L, 6L), recipients(), "source manager, requester, patient - not the actor, not the destination");

        assertEquals(NotificationType.TRANSFER_APPROVED, to(5).getType());
        assertTrue(to(5).getMessage().contains("approved by Meera (B)"));
        assertTrue(to(3).getMessage().contains("Asha Kulkarni (REG-7)"));
        // the patient reads it in the first person and never sees staff details
        String forPatient = to(6).getMessage();
        assertTrue(forPatient.startsWith("Your request"));
        assertTrue(forPatient.contains("approved") && forPatient.contains("Branch B"));
        assertFalse(forPatient.contains("Meera") || forPatient.contains("REG-"));
    }

    @Test void aRejectionSaysSoAndTheStatusPicksTheType() {
        transfer.setStatus(TransferStatus.REJECTED);
        service.transferDecided(transfer, patient, auth(mgrB));
        assertEquals(NotificationType.TRANSFER_REJECTED, to(5).getType());
        assertTrue(to(5).getMessage().contains("rejected"));
        assertTrue(to(6).getMessage().contains("was not approved"));
        assertTrue(to(6).getMessage().contains("stay registered at Branch A"));
    }

    @Test void aDecisionOnAnUndecidedRequestCreatesNothing() {
        transfer.setStatus(TransferStatus.PENDING);
        service.transferDecided(transfer, patient, auth(mgrB));
        service.transferDecided(transfer, patient, auth(mgrB));
        assertTrue(saved.isEmpty());
    }

    @Test void whenThePatientRaisedItAndTheyAreTheActorOnlyTheOthersHearAboutIt() {
        transfer.setRequestedBy("asha@mail.test");
        when(users.findByEmail("asha@mail.test")).thenReturn(Optional.of(patientLogin));
        transfer.setStatus(TransferStatus.APPROVED);
        service.transferDecided(transfer, patient, auth(mgrB));
        assertEquals(Set.of(3L, 6L), recipients());                     // requester == patient: told once, in the patient wording
        assertTrue(to(6).getMessage().startsWith("Your request"));
    }

    @Test void aWithdrawalTellsTheDecidersTheRequesterAndThePatient() {
        service.transferCancelled(transfer, patient, auth(recA));        // the requester withdrew it
        assertEquals(Set.of(1L, 2L, 6L), recipients());
        assertEquals(NotificationType.TRANSFER_CANCELLED, to(2).getType());
        assertTrue(to(2).getMessage().contains("withdrawn"));
        assertTrue(to(6).getMessage().contains("was cancelled"));
    }

    @Test void aPatientWithoutALoginSimplyGetsNoNotification() {
        patient.setUser(null);
        transfer.setStatus(TransferStatus.APPROVED);
        service.transferDecided(transfer, patient, auth(mgrB));
        assertEquals(Set.of(3L, 5L), recipients());
    }

    @Test void aFailureWhileStoringNeverBreaksTheCaller() {
        when(notifications.save(any(Notification.class))).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> service.transferRequested(transfer, patient, auth(recA)));
    }

    // ------------------------------------------------------------ reading: only my own
    private Notification stored(long id, long recipient, boolean read) {
        Notification n = new Notification(recipient, NotificationType.TRANSFER_REQUESTED, "msg " + id, "TRANSFER", 5L);
        ReflectionTestUtils.setField(n, "id", id);
        if (read) n.setReadAt(LocalDateTime.now());
        return n;
    }

    @Test void listingUsesOnlyTheCallersId() {
        when(notifications.findByRecipientUserIdOrderByCreatedAtDesc(eq(2L), any(Pageable.class))).thenReturn(List.of(stored(1, 2, false), stored(2, 2, true)));
        List<NotificationDTO> list = service.list(auth(mgrB), false, null);
        assertEquals(2, list.size());
        assertFalse(list.get(0).read());
        assertTrue(list.get(1).read());
        assertNotNull(list.get(1).readAt());
        assertEquals("TRANSFER_REQUESTED", list.get(0).type());
        assertEquals("msg 1", list.get(0).message());
        assertNotNull(list.get(0).createdAt());
        verify(notifications, never()).findByRecipientUserIdOrderByCreatedAtDesc(eq(1L), any());   // nobody else's rows were asked for
    }

    @Test void unreadOnlyAndTheLimitAreHonouredAndClamped() {
        service.list(auth(mgrB), true, 500);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(notifications).findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(eq(2L), page.capture());
        assertEquals(100, page.getValue().getPageSize());
        service.list(auth(mgrB), false, 0);
        verify(notifications).findByRecipientUserIdOrderByCreatedAtDesc(eq(2L), argThat((Pageable p) -> p.getPageSize() == 1));
    }

    @Test void unreadCountIsPerUser() {
        when(notifications.countByRecipientUserIdAndReadAtIsNull(2L)).thenReturn(3L);
        when(notifications.countByRecipientUserIdAndReadAtIsNull(1L)).thenReturn(9L);
        assertEquals(3L, service.unreadCount(auth(mgrB)));
        assertEquals(9L, service.unreadCount(auth(admin)));
    }

    @Test void markingReadSetsTheTimeOnceAndOnlyForTheOwner() {
        Notification n = stored(10, 2, false);
        when(notifications.findByIdAndRecipientUserId(10L, 2L)).thenReturn(Optional.of(n));
        NotificationDTO dto = service.markRead(auth(mgrB), 10L);
        assertTrue(dto.read());
        LocalDateTime first = n.getReadAt();
        assertNotNull(first);
        service.markRead(auth(mgrB), 10L);                                // idempotent: the first read time stays
        assertEquals(first, n.getReadAt());
        verify(notifications, times(1)).save(n);
    }

    @Test void anotherUsersNotificationLooksLikeItDoesNotExist() {
        // notification 10 belongs to user 2; user 1 asks for it: the owner-keyed lookup finds nothing
        when(notifications.findByIdAndRecipientUserId(10L, 2L)).thenReturn(Optional.of(stored(10, 2, false)));
        ApiException e = assertThrows(ApiException.class, () -> service.markRead(auth(admin), 10L));
        assertEquals(404, e.getStatus());
        verify(notifications, never()).findById(any());
        verify(notifications, never()).save(any());
    }

    @Test void markAllReadTouchesOnlyTheCallersRows() {
        when(notifications.markAllRead(eq(2L), any(LocalDateTime.class))).thenReturn(4);
        assertEquals(4, service.markAllRead(auth(mgrB)));
        verify(notifications).markAllRead(eq(2L), any(LocalDateTime.class));
        verify(notifications, never()).markAllRead(eq(1L), any());
    }

    // ------------------------------------------------------------ links only where the viewer may go
    private String linkFor(AuthUser viewer, Notification n) {
        when(notifications.findByRecipientUserIdOrderByCreatedAtDesc(eq(viewer.id()), any(Pageable.class))).thenReturn(List.of(n));
        when(transfers.findAllById(anyCollection())).thenReturn(List.of(transfer));
        when(patients.findByUserEmail("asha@mail.test")).thenReturn(Optional.of(patient));
        return service.list(viewer, false, null).get(0).link();
    }

    @Test void destinationStaffOpenTheApprovalQueueAndSourceStaffTheOutgoingList() {
        assertEquals("/staff-dashboard/transfers", linkFor(auth(mgrB), stored(1, 2, false)));
        assertEquals("/staff-dashboard/transfers", linkFor(auth(recB), stored(1, 4, false)));
        assertEquals("/staff-dashboard/transfers/outgoing", linkFor(auth(mgrA), stored(1, 3, false)));
        assertEquals("/staff-dashboard/transfers/outgoing", linkFor(auth(recA), stored(1, 5, false)));
        assertEquals("/staff-dashboard/transfers", linkFor(auth(admin), stored(1, 1, false)));
    }

    @Test void noLinkWhenTheViewerHasNoAccessToThatWorkflow() {
        assertNull(linkFor(new AuthUser(8L, "x@x.test", "X", Role.BRANCH_MANAGER, "C"), stored(1, 8, false)));     // unrelated branch
        assertNull(linkFor(new AuthUser(9L, "y@x.test", "Y", Role.RECEPTIONIST, null), stored(1, 9, false)));      // no branch
        assertNull(linkFor(new AuthUser(10L, "d@x.test", "D", Role.DOCTOR, "B"), stored(1, 10, false)));            // doctors never
    }

    @Test void patientsOpenOnlyTheirOwnRequestAndNoLinkWhenItBelongsToSomeoneElse() {
        assertEquals("/dashboard/profile", linkFor(auth(patientLogin), stored(1, 6, false)));
        transfer.setPatientId(99L);                                                                      // someone else's transfer
        assertNull(linkFor(auth(patientLogin), stored(1, 6, false)));
    }

    @Test void aMissingTransferGivesPlainTextNotADeadLink() {
        Notification n = stored(1, 2, false);
        when(notifications.findByRecipientUserIdOrderByCreatedAtDesc(eq(2L), any(Pageable.class))).thenReturn(List.of(n));
        when(transfers.findAllById(anyCollection())).thenReturn(List.of());
        assertNull(service.list(auth(mgrB), false, null).get(0).link());
    }
}
