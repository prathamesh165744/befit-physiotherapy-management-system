package com.befit.backend.dto;

import com.befit.backend.entity.Branch;

public record BranchDTO(String id, String name, String address, String phone, String email, boolean active) {
    public static BranchDTO of(Branch b) {
        return new BranchDTO(b.getId(), b.getName(), b.getAddress(), b.getPhone(), b.getEmail(), b.isActive());
    }
}
