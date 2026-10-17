package com.befit.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Money received against an invoice. A payment is never moved to another invoice or branch.
 * idempotencyKey (unique) stops a double-click / retry from recording the same payment twice.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_bill", columnList = "bill_id"),
        @Index(name = "idx_payments_branch_date", columnList = "branch_id, payment_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_idempotency", columnNames = "idempotency_key")
})
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bill_id", nullable = false) private Bill bill;
    /** Copy of the invoice's branch, so branch-level reports need no join. */
    @Column(name = "branch_id", nullable = false) private String branchId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status = PaymentStatus.COMPLETED;
    @Column(name = "payment_date", nullable = false) private LocalDateTime paymentDate;
    @Column(name = "reference_no", length = 100) private String referenceNo;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(name = "received_by") private String receivedBy;

    public Long getId() { return id; }
    public Bill getBill() { return bill; } public void setBill(Bill bill) { this.bill = bill; }
    public String getBranchId() { return branchId; } public void setBranchId(String v) { this.branchId = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    public PaymentMethod getMethod() { return method; } public void setMethod(PaymentMethod v) { this.method = v; }
    public PaymentStatus getStatus() { return status; } public void setStatus(PaymentStatus v) { this.status = v; }
    public LocalDateTime getPaymentDate() { return paymentDate; } public void setPaymentDate(LocalDateTime v) { this.paymentDate = v; }
    public String getReferenceNo() { return referenceNo; } public void setReferenceNo(String v) { this.referenceNo = v; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String v) { this.idempotencyKey = v; }
    public String getReceivedBy() { return receivedBy; } public void setReceivedBy(String v) { this.receivedBy = v; }
}
