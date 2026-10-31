package com.befit.backend.service;

import com.befit.backend.dto.RegisterRequest;
import com.befit.backend.entity.Branch;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** BEFIT-32: online self-registration must never duplicate a patient the clinic already registered. */
class UserServiceRegistrationTest {
    private UserRepository users;
    private PatientRepository patients;
    private PatientService patientService;
    private UserService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        patients = mock(PatientRepository.class);
        patientService = mock(PatientService.class);
        BranchRepository branches = mock(BranchRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("ENCODED");
        when(branches.findById("KARVE-ROAD")).thenReturn(Optional.of(new Branch("KARVE-ROAD", "Karve Road")));
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(patients.save(any(Patient.class))).thenAnswer(i -> { Patient p = i.getArgument(0); p.setId(1L); return p; });
        service = new UserService();
        ReflectionTestUtils.setField(service, "userRepository", users);
        ReflectionTestUtils.setField(service, "patientRepository", patients);
        ReflectionTestUtils.setField(service, "branchRepository", branches);
        ReflectionTestUtils.setField(service, "historyRepository", mock(PatientBranchHistoryRepository.class));
        ReflectionTestUtils.setField(service, "patientService", patientService);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        when(patientService.findDuplicate(anyString(), anyString(), any())).thenReturn(Optional.empty());
    }

    private static RegisterRequest request() {
        RegisterRequest r = new RegisterRequest();
        r.setFullName("Asha Kulkarni"); r.setEmail("Asha@Mail.test"); r.setPhone("9820000000"); r.setPassword("my-own-password");
        r.setDob(LocalDate.of(1990, 4, 2)); r.setBranchId("KARVE-ROAD");
        return r;
    }

    @Test void aNewPersonRegistersWithALoginAndOneProfile() {
        User u = service.registerPatient(request());
        assertEquals(Role.PATIENT, u.getRole());
        verify(patients, times(1)).save(any(Patient.class));
    }

    @Test void someoneAlreadyRegisteredAtTheClinicCannotCreateASecondProfile() {
        when(patientService.findDuplicate("Asha Kulkarni", "9820000000", LocalDate.of(1990, 4, 2))).thenReturn(Optional.of(new Patient()));
        ApiException e = assertThrows(ApiException.class, () -> service.registerPatient(request()));
        assertEquals("DUPLICATE_PATIENT", e.getCode());
        assertTrue(e.getMessage().toLowerCase().contains("activation link"));
        assertFalse(e.getMessage().contains("REG-"), "nothing about the existing profile is revealed");
        verify(users, never()).save(any());
        verify(patients, never()).save(any());
    }

    @Test void emailUniquenessIgnoresCase() {
        when(users.existsByEmailIgnoreCase("Asha@Mail.test")).thenReturn(true);
        assertEquals("EMAIL_EXISTS", assertThrows(ApiException.class, () -> service.registerPatient(request())).getCode());
        verify(patients, never()).save(any());
    }
}
