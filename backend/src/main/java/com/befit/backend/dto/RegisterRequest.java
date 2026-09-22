package com.befit.backend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String phone;
    private String password;
    
    // New patient demographics and contact info
    private String dob;
    private String gender;
    private String address;
    private String emergencyContactName;
    private String emergencyContactNumber;
    
    // New pain metrics
    private String painType;
    private Integer painRating;
}