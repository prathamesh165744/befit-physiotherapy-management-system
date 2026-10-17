package com.befit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of PUT /api/patients/me/branch and POST /api/patients/{id}/transfer. */
public record TransferRequestBody(@NotBlank(message = "Please choose a branch") String branchId, @Size(max = 500) String reason) {}
