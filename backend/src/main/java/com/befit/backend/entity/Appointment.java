package com.befit.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne @JoinColumn(name = "patient_id", nullable = false) private Patient patient;
    @Column(name = "doctor_id", nullable = false) private Long doctorId; 
    @Column(name = "doctor_name") private String doctorName; 
    @Column(name = "appointment_date") private LocalDateTime appointmentDate;
    @Enumerated(EnumType.STRING) private AppointmentStatus status;
    private String caseType; 
    @Column(name = "pain_type") private String painType;
    @Column(name = "pain_rating") private Integer painRating;
    @Column(name = "branch_id") private String branchId;
    @Column(name = "check_in_time") private LocalDateTime checkInTime;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Patient getPatient() { return patient; } public void setPatient(Patient patient) { this.patient = patient; }
    public Long getDoctorId() { return doctorId; } public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getDoctorName() { return doctorName; } public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public LocalDateTime getAppointmentDate() { return appointmentDate; } public void setAppointmentDate(LocalDateTime appointmentDate) { this.appointmentDate = appointmentDate; }
    public AppointmentStatus getStatus() { return status; } public void setStatus(AppointmentStatus status) { this.status = status; }
    public String getCaseType() { return caseType; } public void setCaseType(String caseType) { this.caseType = caseType; }
    public String getPainType() { return painType; } public void setPainType(String painType) { this.painType = painType; }
    public Integer getPainRating() { return painRating; } public void setPainRating(Integer painRating) { this.painRating = painRating; }
    public String getBranchId() { return branchId; } public void setBranchId(String branchId) { this.branchId = branchId; }
    public LocalDateTime getCheckInTime() { return checkInTime; } public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
}