package com.befit.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Append-only history of everything that happens to an invoice. Rows are never updated or deleted. */
@Entity
@Table(name = "bill_audit_log", indexes = {
        @Index(name = "idx_bill_audit_bill", columnList = "bill_id"),
        @Index(name = "idx_bill_audit_branch", columnList = "branch_id")
})
public class BillAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "bill_id", nullable = false) private Long billId;
    @Column(name = "branch_id") private String branchId;
    /** CREATED, EDITED, PAYMENT_ADDED, CANCELLED */
    @Column(nullable = false, length = 30) private String action;
    @Column(name = "changed_by", nullable = false) private String changedBy;
    @Column(name = "changed_at", nullable = false) private LocalDateTime changedAt;
    @Column(length = 500) private String reason;
    @Column(name = "previous_values", columnDefinition = "TEXT") private String previousValues;
    @Column(name = "new_values", columnDefinition = "TEXT") private String newValues;

    public BillAuditLog() {}
    public BillAuditLog(Long billId, String branchId, String action, String changedBy, String reason,
                        String previousValues, String newValues) {
        this.billId = billId; this.branchId = branchId; this.action = action; this.changedBy = changedBy;
        this.reason = reason; this.previousValues = previousValues; this.newValues = newValues;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getBillId() { return billId; }
    public String getBranchId() { return branchId; }
    public String getAction() { return action; }
    public String getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getReason() { return reason; }
    public String getPreviousValues() { return previousValues; }
    public String getNewValues() { return newValues; }
}
