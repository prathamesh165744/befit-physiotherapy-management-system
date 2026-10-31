package com.befit.backend.dto;

import com.befit.backend.entity.Patient;

import java.time.LocalDate;

/** One row of the patients directory - deliberately no medical history / address. */
public record PatientSummaryDTO(Long id, String regId, String fullName, String phone, String gender,
                                LocalDate dateOfBirth, LocalDate registrationDate, String branchId, String branchName,
                                String portalAccess) {
    public static final String ACTIVE = "ACTIVE";     // has a login: the patient can use the portal
    public static final String INVITED = "INVITED";   // a live activation link was issued, not used yet
    public static final String NONE = "NONE";         // registered by staff only, no portal access

    /** Portal access as far as the patient row itself tells (linked login = ACTIVE, otherwise NONE). */
    public static PatientSummaryDTO of(Patient p, String branchName) {
        return of(p, branchName, p.getUser() != null ? ACTIVE : NONE);
    }

    public static PatientSummaryDTO of(Patient p, String branchName, String portalAccess) {
        return new PatientSummaryDTO(p.getId(), "REG-" + p.getId(), p.getFullName(), p.getPhone(), p.getGender(),
                p.getDateOfBirth(), p.getRegistrationDate(), p.getBranchId(), branchName, portalAccess);
    }
}
