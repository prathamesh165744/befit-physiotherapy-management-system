package com.befit.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills")
public class Bill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "appointment_id") private Long appointmentId;
    private String patientName;
    private String branchId;
    private Double amount;
    private String description;
    private LocalDateTime billDate;
    private String status;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getAppointmentId() { return appointmentId; } public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public String getPatientName() { return patientName; } public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getBranchId() { return branchId; } public void setBranchId(String branchId) { this.branchId = branchId; }
    public Double getAmount() { return amount; } public void setAmount(Double amount) { this.amount = amount; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public LocalDateTime getBillDate() { return billDate; } public void setBillDate(LocalDateTime billDate) { this.billDate = billDate; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
}