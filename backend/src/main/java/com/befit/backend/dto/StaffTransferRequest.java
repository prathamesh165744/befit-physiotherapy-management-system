package com.befit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/transfers: staff asks for a patient to be permanently moved to another branch.
 * fromBranchId is optional: when sent it must be the patient's CURRENT branch (guards against a stale screen);
 * the server always takes the real source from the patient record.
 */
public record StaffTransferRequest(@NotNull(message = "Please choose a patient") Long patientId,
                                   String fromBranchId,
                                   @NotBlank(message = "Please choose the destination branch") String toBranchId,
                                   @NotBlank(message = "Please give a reason for the transfer") @Size(max = 500) String reason) {}
