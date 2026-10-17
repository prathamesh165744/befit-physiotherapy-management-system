package com.befit.backend.dto;

import com.befit.backend.entity.Patient;

import java.time.LocalDate;

/** One row of the patients directory - deliberately no medical history / address. */
public record PatientSummaryDTO(Long id, String regId, String fullName, String phone, String gender,
                                LocalDate dateOfBirth, LocalDate registrationDate, String branchId, String branchName) {
    public static PatientSummaryDTO of(Patient p, String branchName) {
        return new PatientSummaryDTO(p.getId(), "REG-" + p.getId(), p.getFullName(), p.getPhone(), p.getGender(),
                p.getDateOfBirth(), p.getRegistrationDate(), p.getBranchId(), branchName);
    }
}
