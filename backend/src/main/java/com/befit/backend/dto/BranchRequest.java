package com.befit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BranchRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9-]{2,50}$", message = "Branch id must be 2-50 characters: capital letters, digits and dashes") String id,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String address,
        @Size(max = 30) String phone,
        @Size(max = 150) String email,
        Boolean active) {}
