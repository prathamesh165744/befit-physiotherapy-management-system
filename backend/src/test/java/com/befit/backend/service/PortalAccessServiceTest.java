package com.befit.backend.service;

import com.befit.backend.dto.ActivateRequest;
import com.befit.backend.dto.InvitationCheckDTO;
import com.befit.backend.dto.PortalInvitationDTO;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** BEFIT-32: portal access for walk-in patients - invitation, activation, duplicates, and who may do what. */
class PortalAccessServiceTest {
    private PatientRepository patients;
    private UserRepository users;
    private PatientInvitationRepository invitations;
    private PasswordEncoder encoder;
    private PortalAccessService service;
    private Patient patient;
    private final List<PatientInvitation> stored = new ArrayList<>();

    private static final AuthUser RECEPTIONIST_A = new AuthUser(1L, "rec@A.test", "R", Role.RECEPTIONIST, "A");

    @BeforeEach
    void setUp() {
        patients = mock(PatientRepository.class);
        users = mock(UserRepository.class);
        invitations = mock(PatientInvitationRepository.class);
        encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenAnswer(i -> "HASHED:" + i.getArgument(0));
        BranchAccess access = new BranchAccess(mock(BranchRepository.class), mock(AppointmentRepository.class), mock(BillRepository.class),
                mock(PatientBranchHistoryRepository.class), 3);
        service = new PortalAccessService(patients, users, invitations, encoder, access, 48);

        patient = new Patient();
        patient.setId(7L); patient.setFullName("Asha Kulkarni"); patient.setPhone("9820000000"); patient.setBranchId("A");
        when(patients.findById(7L)).thenReturn(Optional.of(patient));
        when(patients.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(invitations.save(any(PatientInvitation.class))).thenAnswer(i -> { stored.add(i.getArgument(0)); return i.getArgument(0); });
        when(invitations.findByPatientIdAndUsedAtIsNullAndRevokedAtIsNull(7L)).thenAnswer(i ->
                stored.stream().filter(x -> x.getUsedAt() == null && x.getRevokedAt() == null).toList());
        when(invitations.findByTokenHash(anyString())).thenAnswer(i -> stored.stream().filter(x -> x.getTokenHash().equals(i.getArgument(0))).findFirst());
        when(invitations.findForUpdateByTokenHash(anyString())).thenAnswer(i -> stored.stream().filter(x -> x.getTokenHash().equals(i.getArgument(0))).findFirst());
    }

    private static ActivateRequest activate(String token, String email, String pw) { return new ActivateRequest(token, email, pw); }

    // ------------------------------------------------------------ creating an invitation
    @Test void homeBranchReceptionistCreatesAnExpiringOneTimeLink() {
        LocalDateTime before = LocalDateTime.now();
        PortalInvitationDTO dto = service.issue(RECEPTIONIST_A, 7L);

        assertTrue(dto.token().length() >= 43, "256-bit random token");
        assertEquals("/activate#t=" + dto.token(), dto.activationPath());
        assertTrue(dto.expiresAt().isAfter(before.plusHours(47)) && dto.expiresAt().isBefore(LocalDateTime.now().plusHours(49)));
        assertEquals(1, stored.size());
        assertNull(patient.getUser(), "inviting does not create a login");
        verify(users, never()).save(any());
    }

    @Test void onlyTheTokensHashIsStored() {
        PortalInvitationDTO dto = service.issue(RECEPTIONIST_A, 7L);
        PatientInvitation inv = stored.get(0);
        assertNotEquals(dto.token(), inv.getTokenHash());
        assertEquals(PortalAccessService.hash(dto.token()), inv.getTokenHash());
        assertEquals(64, inv.getTokenHash().length());
        assertEquals("rec@A.test", inv.getCreatedBy());
    }

    @Test void tokensAreRandom() {
        assertNotEquals(PortalAccessService.newToken(), PortalAccessService.newToken());
    }

    @Test void issuingAgainCancelsTheEarlierLink() {
        PortalInvitationDTO first = service.issue(RECEPTIONIST_A, 7L);
        PortalInvitationDTO second = service.issue(RECEPTIONIST_A, 7L);
        assertNotNull(stored.get(0).getRevokedAt());
        assertNull(stored.get(1).getRevokedAt());
        assertFalse(service.check(first.token()).valid());
        assertTrue(service.check(second.token()).valid());
    }

    @Test void adminMayCreateOneToo() {
        assertNotNull(service.issue(new AuthUser(2L, "admin@x.test", "A", Role.ADMIN, null), 7L).token());
    }

    @Test void onlyReceptionistsAndAdminsOfThePatientsOwnBranchMayInvite() {
        for (AuthUser u : List.of(
                new AuthUser(3L, "rec@B.test", "R", Role.RECEPTIONIST, "B"),        // another branch
                new AuthUser(4L, "doc@A.test", "D", Role.DOCTOR, "A"),
                new AuthUser(5L, "mgr@A.test", "M", Role.BRANCH_MANAGER, "A"),
                new AuthUser(6L, "pat@x.test", "P", Role.PATIENT, "A"))) {
            assertEquals(403, assertThrows(ApiException.class, () -> service.issue(u, 7L)).getStatus(), u.role().name());
            assertEquals(403, assertThrows(ApiException.class, () -> service.revoke(u, 7L)).getStatus(), u.role().name());
        }
        assertTrue(stored.isEmpty());
    }

    @Test void unknownPatientIs404AndPatientWithALoginIs409() {
        assertEquals(404, assertThrows(ApiException.class, () -> service.issue(RECEPTIONIST_A, 99L)).getStatus());
        patient.setUser(new User());
        assertEquals("ALREADY_HAS_ACCOUNT", assertThrows(ApiException.class, () -> service.issue(RECEPTIONIST_A, 7L)).getCode());
        assertTrue(stored.isEmpty());
    }

    @Test void staffCanRevokeAnOpenLink() {
        PortalInvitationDTO dto = service.issue(RECEPTIONIST_A, 7L);
        service.revoke(RECEPTIONIST_A, 7L);
        assertFalse(service.check(dto.token()).valid());
        assertThrows(ApiException.class, () -> service.activate(activate(dto.token(), "asha@mail.test", "secret-pass-1")));
    }

    // ------------------------------------------------------------ redeeming it
    @Test void patientActivatesWithTheirOwnEmailAndPasswordOnTheExistingProfile() {
        String token = service.issue(RECEPTIONIST_A, 7L).token();
        service.activate(activate(token, "  asha@mail.test ", "my-own-password"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        User u = saved.getValue();
        assertEquals(Role.PATIENT, u.getRole());
        assertEquals("asha@mail.test", u.getEmail());
        assertEquals("HASHED:my-own-password", u.getPassword(), "stored encoded, chosen by the patient");
        assertTrue(u.isActive());
        assertSame(u, patient.getUser(), "the login is attached to the EXISTING profile");
        verify(patients, never()).save(argThat((Patient p) -> p != patient));     // no second profile
        assertNotNull(stored.get(0).getUsedAt());
    }

    @Test void nothingIsGeneratedOrReturnedAsACredential() {
        String token = service.issue(RECEPTIONIST_A, 7L).token();
        service.activate(activate(token, "asha@mail.test", "my-own-password"));
        verify(encoder, times(1)).encode("my-own-password");                      // the only password ever encoded is the patient's
    }

    @Test void aLinkWorksOnlyOnce() {
        String token = service.issue(RECEPTIONIST_A, 7L).token();
        service.activate(activate(token, "asha@mail.test", "my-own-password"));
        ApiException e = assertThrows(ApiException.class, () -> service.activate(activate(token, "other@mail.test", "another-password")));
        assertEquals(PortalAccessService.INVALID_LINK, e.getMessage());
        verify(users, times(1)).save(any());
    }

    @Test void expiredLinksAreRefused() {
        String token = service.issue(RECEPTIONIST_A, 7L).token();
        stored.get(0).setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertFalse(service.check(token).valid());
        assertEquals(PortalAccessService.INVALID_LINK, assertThrows(ApiException.class,
                () -> service.activate(activate(token, "asha@mail.test", "my-own-password"))).getMessage());
        verify(users, never()).save(any());
    }

    @Test void guessedOrBlankTokensGetTheSameAnswerAsExpiredOnes() {
        assertEquals(PortalAccessService.INVALID_LINK, assertThrows(ApiException.class,
                () -> service.activate(activate("not-a-real-token", "asha@mail.test", "my-own-password"))).getMessage());
        assertFalse(service.check("not-a-real-token").valid());
        assertFalse(service.check("  ").valid());
        assertFalse(service.check(null).valid());
        assertNull(service.check("nope").firstName());
    }

    @Test void anEmailThatAlreadyHasAnAccountIsRefusedWithoutSpendingTheLink() {
        String token = service.issue(RECEPTIONIST_A, 7L).token();
        when(users.existsByEmailIgnoreCase("taken@mail.test")).thenReturn(true);
        assertEquals("EMAIL_EXISTS", assertThrows(ApiException.class, () -> service.activate(activate(token, "taken@mail.test", "my-own-password"))).getCode());
        assertNull(stored.get(0).getUsedAt());
        assertNull(patient.getUser());
        service.activate(activate(token, "free@mail.test", "my-own-password"));        // can try again with another address
        assertNotNull(patient.getUser());
    }

    @Test void aPatientWhoGotALoginAnotherWayCannotUseTheLink() {
        String token = service.issue(RECEPTIONIST_A, 7L).token();
        patient.setUser(new User());
        assertEquals(PortalAccessService.INVALID_LINK, assertThrows(ApiException.class,
                () -> service.activate(activate(token, "asha@mail.test", "my-own-password"))).getMessage());
        verify(users, never()).save(any());
    }

    @Test void checkOnlyRevealsValidityAndFirstName() {
        String token = service.issue(RECEPTIONIST_A, 7L).token();
        InvitationCheckDTO c = service.check(token);
        assertTrue(c.valid());
        assertEquals("Asha", c.firstName());
    }
}
