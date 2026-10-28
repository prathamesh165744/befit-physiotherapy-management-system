package com.befit.backend.entity;

public enum Role {
    // Staff Portal Roles
    ADMIN,
    /** Runs ONE branch: decides branch-transfer requests into that branch. Branch-scoped, never has admin-wide access. */
    BRANCH_MANAGER,
    DOCTOR,
    RECEPTIONIST,

    // Patient Portal Role
    PATIENT
}
