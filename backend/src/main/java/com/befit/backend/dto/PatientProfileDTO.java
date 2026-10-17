package com.befit.backend.dto;

import com.befit.backend.entity.Patient;

import java.time.LocalDate;

/** Full profile. Never contains the login row / password hash. */
public record PatientProfileDTO(Long id, String regId, String fullName, String phone, String gender, LocalDate dateOfBirth,
                                String address, String emergencyContact, String emergencyContactName, String bloodGroup,
                                String occupation, String medicalHistory, LocalDate registrationDate,
                                String branchId, String branchName, TransferDTO pendingTransfer) {
    public static PatientProfileDTO of(Patient p, String branchName, TransferDTO pending) {
        return new PatientProfileDTO(p.getId(), "REG-" + p.getId(), p.getFullName(), p.getPhone(), p.getGender(),
                p.getDateOfBirth(), p.getAddress(), p.getEmergencyContact(), p.getEmergencyContactName(), p.getBloodGroup(),
                p.getOccupation(), p.getMedicalHistory(), p.getRegistrationDate(), p.getBranchId(), branchName, pending);
    }
}
