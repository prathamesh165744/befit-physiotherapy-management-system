package com.befit.backend.dto;

import com.befit.backend.entity.Appointment;

import java.time.LocalDateTime;

/**
 * Appointment as shown in lists. crossBranch = the visit is at a branch other than the patient's registered one
 * (patientHomeBranchId); the appointment's own branchId is always where the visit happens.
 * patient{id,fullName} keeps the shape the existing frontend already reads. */
public record AppointmentDTO(Long id, PatientRef patient, Long doctorId, String doctorName, LocalDateTime appointmentDate,
                             String status, String caseType, String branchId, String branchName, LocalDateTime checkInTime,
                             String patientHomeBranchId, boolean crossBranch) {
    public record PatientRef(Long id, String fullName) {}

    public static AppointmentDTO of(Appointment a, String branchName) {
        String home = a.getPatient() == null ? null : a.getPatient().getBranchId();
        PatientRef ref = a.getPatient() == null ? null : new PatientRef(a.getPatient().getId(), a.getPatient().getFullName());
        return new AppointmentDTO(a.getId(), ref, a.getDoctorId(), a.getDoctorName(), a.getAppointmentDate(),
                a.getStatus() == null ? null : a.getStatus().name(), a.getCaseType(), a.getBranchId(), branchName, a.getCheckInTime(),
                home, home != null && a.getBranchId() != null && !home.equals(a.getBranchId()));
    }
}
