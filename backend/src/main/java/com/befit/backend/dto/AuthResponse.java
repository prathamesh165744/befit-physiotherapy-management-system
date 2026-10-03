package com.befit.backend.dto;
import com.befit.backend.entity.Role;

public class AuthResponse {
    private String token;
    private Role role;
    private String fullName;
    private String branchId; // Added to send to React

    public AuthResponse(String token, Role role, String fullName, String branchId) {
        this.token = token;
        this.role = role;
        this.fullName = fullName;
        this.branchId = branchId;
    }

    public String getToken() { return token; } public void setToken(String token) { this.token = token; }
    public Role getRole() { return role; } public void setRole(Role role) { this.role = role; }
    public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; }
    public String getBranchId() { return branchId; } public void setBranchId(String branchId) { this.branchId = branchId; }
}