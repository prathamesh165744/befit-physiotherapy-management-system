package com.befit.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class RegisterRequest {
    @NotBlank(message = "Full name is required") @Size(max = 150) private String fullName;
    @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) private String email;
    @NotBlank(message = "Phone number is required") @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Enter a valid phone number") private String phone;
    @NotBlank(message = "Password is required") @Size(min = 8, max = 100, message = "Password must be at least 8 characters") private String password;
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