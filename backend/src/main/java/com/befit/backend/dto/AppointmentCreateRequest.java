package com.befit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/appointments. appointmentDate is an ISO local date-time, e.g. 2026-10-20T10:30:00.
 *
 * branchId is OPTIONAL and means "the branch where the visit takes place". Left out (or equal to the patient's home
 * branch) it is a normal booking at the patient's current branch. A DIFFERENT branch makes it a cross-branch
 * (temporary) visit: the patient stays registered at the home branch, only this appointment belongs to the other branch.
 * The server checks the caller may book there (receptionist of that branch, or admin); it never trusts the value alone.
 */
public record AppointmentCreateRequest(
        @NotNull(message = "Please choose a patient") Long patientId,
        Long doctorId,
        @NotBlank(message = "Please choose a doctor") @Size(max = 150) String doctorName,
        @NotBlank(message = "Please choose a date and time") String appointmentDate,
        @Size(max = 100) String caseType,
        @Size(max = 50) String branchId) {}
