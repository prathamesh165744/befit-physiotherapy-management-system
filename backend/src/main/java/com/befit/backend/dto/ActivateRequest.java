package com.befit.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/auth/activate: the patient redeems an invitation and chooses their own login e-mail and password. */
public record ActivateRequest(
        @NotBlank(message = "The activation link is missing") @Size(max = 200) String token,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) String email,
        @NotBlank(message = "Password is required") @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String password) {}
