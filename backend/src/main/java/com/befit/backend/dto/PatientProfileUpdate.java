package com.befit.backend.dto;

/** Fields a patient is allowed to edit about themselves. Branch is changed through its own endpoint. */
public class PatientProfileUpdate {
    private String fullName, phone, gender, address, emergencyContact, emergencyContactName, bloodGroup, occupation;

    public String getFullName() { return fullName; } public void setFullName(String v) { this.fullName = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { this.phone = v; }
    public String getGender() { return gender; } public void setGender(String v) { this.gender = v; }
    public String getAddress() { return address; } public void setAddress(String v) { this.address = v; }
    public String getEmergencyContact() { return emergencyContact; } public void setEmergencyContact(String v) { this.emergencyContact = v; }
    public String getEmergencyContactName() { return emergencyContactName; } public void setEmergencyContactName(String v) { this.emergencyContactName = v; }
    public String getBloodGroup() { return bloodGroup; } public void setBloodGroup(String v) { this.bloodGroup = v; }
    public String getOccupation() { return occupation; } public void setOccupation(String v) { this.occupation = v; }
}
