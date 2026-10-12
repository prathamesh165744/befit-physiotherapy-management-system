package com.befit.backend.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * All patient profile + health data. One table for all branches, split by branch_id (indexed).
 * branchId = the patient's CURRENT branch. Past branches are in patient_branch_history.
 */
@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patients_branch", columnList = "branch_id"),
        @Index(name = "idx_patients_phone", columnList = "phone"),
        @Index(name = "idx_patients_user", columnList = "user_id")
})
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String emergencyContact;               // kept: used by the frontend
    @Column(name = "emergency_contact_name") private String emergencyContactName;
    @Column(columnDefinition = "TEXT") private String address;
    @Column(name = "branch_id") private String branchId;
    @Column(name = "blood_group") private String bloodGroup;
    private String occupation;
    @Column(columnDefinition = "TEXT") private String medicalHistory;
    @Column(name = "registration_date") private LocalDate registrationDate = LocalDate.now();
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    // JsonIgnore: never send the login row (it contains the password hash) to the browser
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;

    @PrePersist @PreUpdate
    protected void touch() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getGender() { return gender; } public void setGender(String gender) { this.gender = gender; }
    public LocalDate getDateOfBirth() { return dateOfBirth; } public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getEmergencyContact() { return emergencyContact; } public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getEmergencyContactName() { return emergencyContactName; } public void setEmergencyContactName(String v) { this.emergencyContactName = v; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public String getBranchId() { return branchId; } public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getBloodGroup() { return bloodGroup; } public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getOccupation() { return occupation; } public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getMedicalHistory() { return medicalHistory; } public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
    public LocalDate getRegistrationDate() { return registrationDate; } public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public User getUser() { return user; } public void setUser(User user) { this.user = user; }
}
