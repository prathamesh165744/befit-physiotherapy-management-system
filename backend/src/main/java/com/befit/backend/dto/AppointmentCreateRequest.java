package com.befit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/appointments. appointmentDate is an ISO local date-time, e.g. 2026-10-20T10:30:00.
 * There is no branchId here on purpose: the appointment is booked at the patient's CURRENT branch.
 */
public record AppointmentCreateRequest(
        @NotNull(message = "Please choose a patient") Long patientId,
        Long doctorId,
        @NotBlank(message = "Please choose a doctor") @Size(max = 150) String doctorName,
        @NotBlank(message = "Please choose a date and time") String appointmentDate,
        @Size(max = 100) String caseType) {}
