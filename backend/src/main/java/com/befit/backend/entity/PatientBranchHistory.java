package com.befit.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Which branch a patient belonged to, and when. The row with toDate == null is the current branch.
 * Only the PATIENT's "home branch" is tracked here - appointments and invoices keep their own branch_id forever.
 */
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
    /** Who authorised the move into this branch (email). Null for the first/registration stint. */
    @Column(name = "authorized_by") private String authorizedBy;
    @Column(name = "reason", length = 500) private String reason;

    public PatientBranchHistory() {}
    public PatientBranchHistory(Long patientId, String branchId) {
        this.patientId = patientId; this.branchId = branchId; this.fromDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long v) { this.patientId = v; }
    public String getBranchId() { return branchId; } public void setBranchId(String v) { this.branchId = v; }
    public LocalDateTime getFromDate() { return fromDate; } public void setFromDate(LocalDateTime v) { this.fromDate = v; }
    public LocalDateTime getToDate() { return toDate; } public void setToDate(LocalDateTime v) { this.toDate = v; }
    public String getAuthorizedBy() { return authorizedBy; } public void setAuthorizedBy(String v) { this.authorizedBy = v; }
    public String getReason() { return reason; } public void setReason(String v) { this.reason = v; }
}
