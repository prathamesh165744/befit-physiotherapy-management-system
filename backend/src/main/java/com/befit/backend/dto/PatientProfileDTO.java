package com.befit.backend.dto;

import com.befit.backend.entity.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Profile of one patient. Never contains the login row / password hash.
 *
 * accessLevel tells the UI what it is looking at: SELF (the patient), FULL (home-branch staff / admin), or
 * TEMPORARY_VISIT / HISTORICAL (limited view for another branch's staff). In a limited view the sensitive fields below
 * are null because the server never put them in the response - the UI hiding them is only cosmetic.
 */
public record PatientProfileDTO(Long id, String regId, String fullName, String phone, String gender, LocalDate dateOfBirth,
                                String address, String emergencyContact, String emergencyContactName, String bloodGroup,
                                String occupation, String medicalHistory, LocalDate registrationDate,
                                String branchId, String branchName, TransferDTO pendingTransfer, String accessLevel,
                                String portalAccess, LocalDateTime portalInviteExpiresAt) {

    public static final String SELF = "SELF";
    public static final String FULL = "FULL";

    /** Full profile (the patient themself, home-branch staff, admins). */
    public static PatientProfileDTO of(Patient p, String branchName, TransferDTO pending, String accessLevel) {
        return of(p, branchName, pending, accessLevel, p.getUser() != null ? PatientSummaryDTO.ACTIVE : PatientSummaryDTO.NONE, null);
    }

    /** Full profile with the portal-access state staff need (ACTIVE / INVITED until portalInviteExpiresAt / NONE). */
    public static PatientProfileDTO of(Patient p, String branchName, TransferDTO pending, String accessLevel,
                                       String portalAccess, LocalDateTime portalInviteExpiresAt) {
        return new PatientProfileDTO(p.getId(), "REG-" + p.getId(), p.getFullName(), p.getPhone(), p.getGender(),
                p.getDateOfBirth(), p.getAddress(), p.getEmergencyContact(), p.getEmergencyContactName(), p.getBloodGroup(),
                p.getOccupation(), p.getMedicalHistory(), p.getRegistrationDate(), p.getBranchId(), branchName, pending, accessLevel,
                portalAccess, portalInviteExpiresAt);
    }

    /**
     * Limited profile for a branch that is not the patient's home branch: only what the front desk / doctor needs to
     * recognise the person (REG id, name, gender, date of birth, masked phone) and where they are registered.
     * No address, emergency contact, blood group, occupation, medical history, registration date or transfer info.
     */
    public static PatientProfileDTO limited(Patient p, String homeBranchName, String accessLevel) {
        return new PatientProfileDTO(p.getId(), "REG-" + p.getId(), p.getFullName(), CrossBranchPatientDTO.maskPhone(p.getPhone()),
                p.getGender(), p.getDateOfBirth(), null, null, null, null, null, null, null,
                p.getBranchId(), homeBranchName, null, accessLevel, null, null);
    }
}
