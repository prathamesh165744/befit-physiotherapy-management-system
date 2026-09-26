package com.befit.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String emergencyContact;
    @Column(name = "branch_id") private String branchId;
    @Column(name = "blood_group") private String bloodGroup;
    private String occupation;
    @Column(columnDefinition = "TEXT") private String medicalHistory;
    @Column(name = "registration_date") private LocalDate registrationDate = LocalDate.now();
    @ManyToOne @JoinColumn(name = "user_id") private User user;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getGender() { return gender; } public void setGender(String gender) { this.gender = gender; }
    public LocalDate getDateOfBirth() { return dateOfBirth; } public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getEmergencyContact() { return emergencyContact; } public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getBranchId() { return branchId; } public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getBloodGroup() { return bloodGroup; } public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getOccupation() { return occupation; } public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getMedicalHistory() { return medicalHistory; } public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
    public LocalDate getRegistrationDate() { return registrationDate; } public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }
    public User getUser() { return user; } public void setUser(User user) { this.user = user; }
}