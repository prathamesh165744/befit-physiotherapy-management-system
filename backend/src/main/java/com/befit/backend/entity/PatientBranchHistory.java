package com.befit.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Which branch a patient belonged to, and when. The row with toDate == null is the current branch. */
@Entity
@Table(name = "patient_branch_history",
       indexes = { @Index(name = "idx_pbh_patient", columnList = "patient_id"),
                   @Index(name = "idx_pbh_branch", columnList = "branch_id") })
public class PatientBranchHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "patient_id", nullable = false) private Long patientId;
    @Column(name = "branch_id", nullable = false) private String branchId;
    @Column(name = "from_date", nullable = false) private LocalDateTime fromDate;
    @Column(name = "to_date") private LocalDateTime toDate;

    public PatientBranchHistory() {}
    public PatientBranchHistory(Long patientId, String branchId) {
        this.patientId = patientId; this.branchId = branchId; this.fromDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long v) { this.patientId = v; }
    public String getBranchId() { return branchId; } public void setBranchId(String v) { this.branchId = v; }
    public LocalDateTime getFromDate() { return fromDate; } public void setFromDate(LocalDateTime v) { this.fromDate = v; }
    public LocalDateTime getToDate() { return toDate; } public void setToDate(LocalDateTime v) { this.toDate = v; }
}
