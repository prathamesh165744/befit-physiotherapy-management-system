package com.befit.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String password;
    @Enumerated(EnumType.STRING) private Role role;
    private String dob;
    private String gender;
    private String address;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private String painType;
    private Integer painRating;
    private String branchId; 
    
    // FIX: Tell Java to explicitly map this to PostgreSQL's "is_active" column!
    @Column(name = "is_active", columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; } public void setRole(Role role) { this.role = role; }
    public String getDob() { return dob; } public void setDob(String dob) { this.dob = dob; }
    public String getGender() { return gender; } public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public String getEmergencyContactName() { return emergencyContactName; } public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactNumber() { return emergencyContactNumber; } public void setEmergencyContactNumber(String emergencyContactNumber) { this.emergencyContactNumber = emergencyContactNumber; }
    public String getPainType() { return painType; } public void setPainType(String painType) { this.painType = painType; }
    public Integer getPainRating() { return painRating; } public void setPainRating(Integer painRating) { this.painRating = painRating; }
    public String getBranchId() { return branchId; } public void setBranchId(String branchId) { this.branchId = branchId; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}