package com.befit.backend.security;

import com.befit.backend.entity.Role;

/** The logged-in person, rebuilt from the database on every request (never trusted from the token alone). */
public record AuthUser(Long id, String email, String fullName, Role role, String branchId) {
    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isBranchManager() { return role == Role.BRANCH_MANAGER; }
    public boolean isPatient() { return role == Role.PATIENT; }
}
