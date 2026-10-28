package com.befit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/patients/me/branch (patient) and POST /api/patients/{id}/transfer (admin, immediate).
 * acknowledgeWarnings only matters for the admin's immediate transfer: it confirms the validation warnings were read.
 */
public record TransferRequestBody(@NotBlank(message = "Please choose a branch") String branchId, @Size(max = 500) String reason,
                                  Boolean acknowledgeWarnings) {}
