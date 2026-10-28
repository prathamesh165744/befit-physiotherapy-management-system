package com.befit.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A patient's request to move to another branch, and what happened to it. Kept forever as the transfer audit trail. */
@Entity
@Table(name = "branch_transfer_requests", indexes = {
        @Index(name = "idx_transfer_patient", columnList = "patient_id"),
        @Index(name = "idx_transfer_to_status", columnList = "to_branch_id, status")
})
public class BranchTransferRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "patient_id", nullable = false) private Long patientId;
    @Column(name = "from_branch_id", nullable = false) private String fromBranchId;
    @Column(name = "to_branch_id", nullable = false) private String toBranchId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TransferStatus status = TransferStatus.PENDING;
    @Column(length = 500) private String reason;
    @Column(name = "requested_by") private String requestedBy;
    @Column(name = "requested_by_role", length = 20) private String requestedByRole;      // PATIENT / RECEPTIONIST / ADMIN
    @Column(name = "requested_at", nullable = false) private LocalDateTime requestedAt = LocalDateTime.now();
    @Column(name = "decided_by") private String decidedBy;
    @Column(name = "decided_at") private LocalDateTime decidedAt;
    @Column(name = "decision_note", length = 500) private String decisionNote;
    /** Validation warnings the approver acknowledged when approving (open future visits, unpaid invoices...). Audit trail. */
    @Column(name = "decision_warnings", length = 1000) private String decisionWarnings;
    /**
     * Financial snapshot taken when the transfer was approved: how much was still due and on which invoices.
     * Those invoices stay with their original branch, which keeps the right to collect them. Null = nothing was due.
     */
    @Column(name = "outstanding_amount", precision = 12, scale = 2) private java.math.BigDecimal outstandingAmount;
    @Column(name = "outstanding_invoices") private Integer outstandingInvoices;
    @Column(name = "outstanding_invoice_refs", length = 500) private String outstandingInvoiceRefs;

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long v) { this.patientId = v; }
    public String getFromBranchId() { return fromBranchId; } public void setFromBranchId(String v) { this.fromBranchId = v; }
    public String getToBranchId() { return toBranchId; } public void setToBranchId(String v) { this.toBranchId = v; }
    public TransferStatus getStatus() { return status; } public void setStatus(TransferStatus v) { this.status = v; }
    public String getReason() { return reason; } public void setReason(String v) { this.reason = v; }
    public String getRequestedBy() { return requestedBy; } public void setRequestedBy(String v) { this.requestedBy = v; }
    public java.math.BigDecimal getOutstandingAmount() { return outstandingAmount; } public void setOutstandingAmount(java.math.BigDecimal v) { this.outstandingAmount = v; }
    public Integer getOutstandingInvoices() { return outstandingInvoices; } public void setOutstandingInvoices(Integer v) { this.outstandingInvoices = v; }
    public String getOutstandingInvoiceRefs() { return outstandingInvoiceRefs; } public void setOutstandingInvoiceRefs(String v) { this.outstandingInvoiceRefs = v; }
    public String getRequestedByRole() { return requestedByRole; } public void setRequestedByRole(String v) { this.requestedByRole = v; }
    public String getDecisionWarnings() { return decisionWarnings; } public void setDecisionWarnings(String v) { this.decisionWarnings = v; }
    public LocalDateTime getRequestedAt() { return requestedAt; } public void setRequestedAt(LocalDateTime v) { this.requestedAt = v; }
    public String getDecidedBy() { return decidedBy; } public void setDecidedBy(String v) { this.decidedBy = v; }
    public LocalDateTime getDecidedAt() { return decidedAt; } public void setDecidedAt(LocalDateTime v) { this.decidedAt = v; }
    public String getDecisionNote() { return decisionNote; } public void setDecisionNote(String v) { this.decisionNote = v; }
}
