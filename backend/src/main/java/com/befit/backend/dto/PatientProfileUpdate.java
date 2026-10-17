package com.befit.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Fields a patient is allowed to edit about themselves. Branch is changed through its own endpoint. */
public class PatientProfileUpdate {
    @Size(min = 2, max = 150, message = "Name must be 2-150 characters") private String fullName;
    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,20}$", message = "Enter a valid phone number") private String phone;
    @Size(max = 20) private String gender;
    @Size(max = 1000) private String address;
    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,20}$", message = "Enter a valid emergency contact number") private String emergencyContact;
    @Size(max = 150) private String emergencyContactName;
    @Size(max = 10) private String bloodGroup;
    @Size(max = 100) private String occupation;

    public String getFullName() { return fullName; } public void setFullName(String v) { this.fullName = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { this.phone = v; }
    public String getGender() { return gender; } public void setGender(String v) { this.gender = v; }
    public String getAddress() { return address; } public void setAddress(String v) { this.address = v; }
    public String getEmergencyContact() { return emergencyContact; } public void setEmergencyContact(String v) { this.emergencyContact = v; }
    public String getEmergencyContactName() { return emergencyContactName; } public void setEmergencyContactName(String v) { this.emergencyContactName = v; }
    public String getBloodGroup() { return bloodGroup; } public void setBloodGroup(String v) { this.bloodGroup = v; }
    public String getOccupation() { return occupation; } public void setOccupation(String v) { this.occupation = v; }
}
