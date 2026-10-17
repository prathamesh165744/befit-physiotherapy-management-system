package com.befit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Body of POST /api/patients (a receptionist registering a walk-in patient). The branch is NOT taken from here for receptionists. */
public record PatientRegistrationRequest(
        @NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Enter a valid phone number") String phone,
        @Size(max = 20) String gender,
        @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,20}$", message = "Enter a valid emergency contact number") String emergencyContact,
        @Size(max = 150) String emergencyContactName,
        @Size(max = 1000) String address,
        String branchId) {}
