package com.befit.backend.service;

import com.befit.backend.dto.PatientProfileDTO;
import com.befit.backend.dto.PatientRegistrationRequest;
import com.befit.backend.dto.PatientSummaryDTO;
import com.befit.backend.dto.PageResponse;
import com.befit.backend.entity.*;
import com.befit.backend.repository.*;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** BEFIT-32: staff can tell, per patient, whether the portal is available (ACTIVE / INVITED / NONE). */
class PatientPortalStatusTest {
    private PatientRepository patients;
    private PatientInvitationRepository invitations;
    private PatientService service;
    private static final AuthUser REC_A = new AuthUser(1L, "rec@A.test", "R", Role.RECEPTIONIST, "A");

    @BeforeEach
    void setUp() {
        patients = mock(PatientRepository.class);
        invitations = mock(PatientInvitationRepository.class);
        BranchService branchService = mock(BranchService.class);
        when(branchService.nameMap()).thenReturn(Map.of("A", "Branch A", "B", "Branch B"));
        TransferService transfers = mock(TransferService.class);
        BranchRepository branches = mock(BranchRepository.class);
        when(branches.findAllIds()).thenReturn(List.of("A", "B"));
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        BillRepository bills = mock(BillRepository.class);
        PatientBranchHistoryRepository history = mock(PatientBranchHistoryRepository.class);
        BranchAccess access = new BranchAccess(branches, appointments, bills, history, 3);
        service = new PatientService(patients, history, appointments, bills, branches, branchService, transfers, access, invitations);
    }

    private static Patient patient(long id, String branch, boolean hasLogin) {
        Patient p = new Patient();
        p.setId(id); p.setFullName("Patient " + id); p.setBranchId(branch); p.setPhone("98200000" + id);
        if (hasLogin) p.setUser(new User());
        return p;
    }

    @Test void directoryMarksEachPatientActiveInvitedOrNone() {
        Patient withLogin = patient(1, "A", true), invited = patient(2, "A", false), plain = patient(3, "A", false);
        when(patients.searchCurrent(anyCollection(), anyString(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(withLogin, invited, plain)));
        when(invitations.activeInvitedPatientIds(anyCollection(), any(LocalDateTime.class))).thenReturn(List.of(2L));

        PageResponse<PatientSummaryDTO> page = service.list(REC_A, null, "current", null, 0, 20, "id", "desc");
        assertEquals(List.of("ACTIVE", "INVITED", "NONE"), page.items().stream().map(PatientSummaryDTO::portalAccess).toList());
        // only patients WITHOUT a login are looked up for invitations (one query for the page)
        verify(invitations).activeInvitedPatientIds(eq(List.of(2L, 3L)), any(LocalDateTime.class));
    }

    @Test void directoryAsksNoInvitationQuestionsWhenEveryoneHasALogin() {
        when(patients.searchCurrent(anyCollection(), anyString(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(patient(1, "A", true))));
        service.list(REC_A, null, "current", null, 0, 20, "id", "desc");
        verifyNoInteractions(invitations);
    }

    @Test void profileShowsActiveInvitedWithExpiryOrNone() {
        Patient withLogin = patient(1, "A", true), invited = patient(2, "A", false), plain = patient(3, "A", false);
        when(patients.findById(1L)).thenReturn(Optional.of(withLogin));
        when(patients.findById(2L)).thenReturn(Optional.of(invited));
        when(patients.findById(3L)).thenReturn(Optional.of(plain));
        LocalDateTime expiry = LocalDateTime.now().plusHours(40);
        PatientInvitation inv = new PatientInvitation(2L, "h", "rec@A.test", expiry);
        when(invitations.findActive(eq(2L), any(LocalDateTime.class))).thenReturn(List.of(inv));

        PatientProfileDTO a = service.getForStaff(REC_A, 1L), b = service.getForStaff(REC_A, 2L), c = service.getForStaff(REC_A, 3L);
        assertEquals("ACTIVE", a.portalAccess());
        assertEquals("INVITED", b.portalAccess());
        assertEquals(expiry, b.portalInviteExpiresAt());
        assertEquals("NONE", c.portalAccess());
        assertNull(c.portalInviteExpiresAt());
    }

    @Test void limitedViewsOfOtherBranchesDoNotRevealPortalAccess() {
        Patient p = patient(4, "B", false);
        when(patients.findById(4L)).thenReturn(Optional.of(p));
        // staff of branch A with a past visit get the limited view
        // (modelled through the historical rule: an appointment at A)
        PatientProfileDTO limited = PatientProfileDTO.limited(p, "Branch B", "HISTORICAL");
        assertNull(limited.portalAccess());
        assertNull(limited.portalInviteExpiresAt());
    }

    @Test void walkInRegistrationCreatesNoLoginAndNoCredentials() {
        when(patients.save(any(Patient.class))).thenAnswer(i -> { Patient p = i.getArgument(0); p.setId(9L); return p; });
        when(patients.findByNameAndPhonePattern(anyString(), anyString(), any(Pageable.class))).thenReturn(List.of());
        PatientSummaryDTO dto = service.register(REC_A, new PatientRegistrationRequest("Rahul Kulkarni", "9811111111", "M", null, null, null, null, null));
        assertEquals("NONE", dto.portalAccess());
        verify(patients).save(argThat((Patient p) -> p.getUser() == null && "A".equals(p.getBranchId())));
    }
}
