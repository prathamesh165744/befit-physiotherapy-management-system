package com.befit.backend.service;

import com.befit.backend.entity.Branch;
import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.BranchRepository;
import com.befit.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Creating BRANCH_MANAGER accounts: always bound to one active branch, never an admin. */
class UserServiceBranchManagerTest {
    private UserRepository users;
    private BranchRepository branches;
    private UserService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        branches = mock(BranchRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("ENCODED");
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(branches.findById("B")).thenReturn(Optional.of(new Branch("B", "Branch B")));
        service = new UserService();
        ReflectionTestUtils.setField(service, "userRepository", users);
        ReflectionTestUtils.setField(service, "branchRepository", branches);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
    }

    @Test void createsAManagerTiedToOneBranch() {
        User m = service.createBranchManager("Meera Joshi", " Meera@BeFit.com ", "9820000000", "secret123", "B");
        assertEquals(Role.BRANCH_MANAGER, m.getRole());
        assertEquals("B", m.getBranchId());
        assertEquals("meera@befit.com", m.getEmail());
        assertEquals("ENCODED", m.getPassword());
        assertTrue(m.isActive());
        assertNotEquals(Role.ADMIN, m.getRole());
        assertNotNull(m.getBranchId(), "a manager must never get the null branch that means 'all branches'");
    }

    @Test void branchIsRequiredAndMustExistAndBeActive() {
        assertEquals(400, assertThrows(ApiException.class, () -> service.createBranchManager("M", "m@x.com", "9820000000", "secret123", null)).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> service.createBranchManager("M", "m@x.com", "9820000000", "secret123", " ")).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> service.createBranchManager("M", "m@x.com", "9820000000", "secret123", "NOPE")).getStatus());
        Branch off = new Branch("B", "Branch B"); off.setActive(false);
        when(branches.findById("B")).thenReturn(Optional.of(off));
        assertEquals(400, assertThrows(ApiException.class, () -> service.createBranchManager("M", "m@x.com", "9820000000", "secret123", "B")).getStatus());
        verify(users, never()).save(any());
    }

    @Test void duplicateEmailIsRejected() {
        when(users.findByEmail("m@x.com")).thenReturn(Optional.of(new User()));
        assertEquals("EMAIL_EXISTS", assertThrows(ApiException.class,
                () -> service.createBranchManager("M", "M@x.com", "9820000000", "secret123", "B")).getCode());
    }
}
