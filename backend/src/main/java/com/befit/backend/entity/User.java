package com.befit.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Login identity for everyone (ADMIN / BRANCH_MANAGER / DOCTOR / RECEPTIONIST / PATIENT).
 * Patient health + profile data does NOT live here any more - see Patient.
 * For staff, branchId is their working branch (null = admin, sees all branches). A BRANCH_MANAGER must have one and never sees more.
 */
@Entity
@Table(name = "users", indexes = { @Index(name = "idx_users_email", columnList = "email") })
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String password;
    @Enumerated(EnumType.STRING) private Role role;
    private String branchId;

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // FIX for: null value in column "updated_at" violates not-null constraint
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; } public void setRole(Role role) { this.role = role; }
    public String getBranchId() { return branchId; } public void setBranchId(String branchId) { this.branchId = branchId; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
