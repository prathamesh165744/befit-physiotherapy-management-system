package com.befit.backend.dto;

import com.befit.backend.entity.Patient;

/**
 * One row of the cross-branch patient search. Deliberately minimal: just enough to recognise "this is the same person"
 * and see where they are registered. NO full phone number, address, emergency contact, medical history or visits/bills.
 *
 * @param viewable true when the caller may open this patient's file (own branch, or an admin)
 */
public record CrossBranchPatientDTO(Long id, String regId, String fullName, String maskedPhone, String gender,
                                    Integer birthYear, String branchId, String branchName, boolean viewable) {

    public static CrossBranchPatientDTO of(Patient p, String branchName, boolean viewable) {
        return new CrossBranchPatientDTO(p.getId(), "REG-" + p.getId(), p.getFullName(), maskPhone(p.getPhone()),
                p.getGender(), p.getDateOfBirth() == null ? null : p.getDateOfBirth().getYear(),
                p.getBranchId(), branchName, viewable);
    }

    /** "+91 98765 43210" -> "******3210" */
    static String maskPhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;
        return digits.length() <= 4 ? digits : "******" + digits.substring(digits.length() - 4);
    }
}