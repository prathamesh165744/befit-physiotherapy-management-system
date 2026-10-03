package com.befit.backend.dto;

import java.time.LocalDate;

public class RegisterRequest {
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private LocalDate dob;
    private String gender;
    private String address;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private String painType;
    private Integer painRating;
    private String branchId; // NEW FIELD: Branch Selection

    public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
    public LocalDate getDob() { return dob; } public void setDob(LocalDate dob) { this.dob = dob; }
    public String getGender() { return gender; } public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public String getEmergencyContactName() { return emergencyContactName; } public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactNumber() { return emergencyContactNumber; } public void setEmergencyContactNumber(String emergencyContactNumber) { this.emergencyContactNumber = emergencyContactNumber; }
    public String getPainType() { return painType; } public void setPainType(String painType) { this.painType = painType; }
    public Integer getPainRating() { return painRating; } public void setPainRating(Integer painRating) { this.painRating = painRating; }
    public String getBranchId() { return branchId; } public void setBranchId(String branchId) { this.branchId = branchId; }
}