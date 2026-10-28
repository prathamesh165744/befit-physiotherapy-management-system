package com.befit.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** POST /api/staff/managers (admin only): creates a BRANCH_MANAGER account tied to exactly one branch. */
public record ManagerRequest(
        @NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) String email,
        @NotBlank(message = "Phone number is required") @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Enter a valid phone number") String phone,
        @NotBlank(message = "Password is required") @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String password,
        @NotBlank(message = "Please choose the branch this manager runs") String branchId) {}
