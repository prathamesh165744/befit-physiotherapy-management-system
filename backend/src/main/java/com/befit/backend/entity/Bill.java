package com.befit.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An invoice. branchId is the branch that ISSUED it and never changes, even if the patient later
 * moves to another branch.
 *
 * Money columns are NUMERIC(12,2). The old "amount" (double) column is kept and mirrored from
 * totalAmount so rows/clients created before this change keep working. Columns added by this change
 * are nullable in the database so existing rows survive; DataFixer backfills them on startup.
 */
@Entity
@Table(name = "bills", indexes = {
        @Index(name = "idx_bill_branch_date", columnList = "branch_id, bill_date"),
        @Index(name = "idx_bill_patient", columnList = "patient_id"),
        @Index(name = "idx_bill_branch_status", columnList = "branch_id, status"),
        @Index(name = "idx_bill_appointment", columnList = "appointment_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_bill_invoice_number", columnNames = "invoice_number")
})
public class Bill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "invoice_number", length = 80) private String invoiceNumber;
    @Column(name = "appointment_id") private Long appointmentId;
    @Column(name = "patient_id") private Long patientId;
    private String patientName;
    @Column(name = "branch_id") private String branchId;

    /** Legacy column, mirrors totalAmount. Do not use for new logic. */
    private Double amount;

    private String description;
    @Column(name = "bill_date") private LocalDateTime billDate;
    @Enumerated(EnumType.STRING) private BillStatus status;

    @Column(name = "subtotal", precision = 12, scale = 2) private BigDecimal subtotal;
    @Column(name = "discount_amount", precision = 12, scale = 2) private BigDecimal discountAmount;
    @Column(name = "tax_percent", precision = 5, scale = 2) private BigDecimal taxPercent;
    @Column(name = "tax_amount", precision = 12, scale = 2) private BigDecimal taxAmount;
    @Column(name = "total_amount", precision = 12, scale = 2) private BigDecimal totalAmount;
    @Column(name = "amount_paid", precision = 12, scale = 2) private BigDecimal amountPaid;
    /** Method of the payment(s): a single method name, or MIXED when several were used. Null = nothing paid. */
    @Column(name = "payment_method", length = 20) private String paymentMethod;

    @Column(name = "revision") private Integer revision;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "updated_by") private String updatedBy;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; } public void setInvoiceNumber(String v) { this.invoiceNumber = v; }
    public Long getAppointmentId() { return appointmentId; } public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; } public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getBranchId() { return branchId; } public void setBranchId(String branchId) { this.branchId = branchId; }
    public Double getAmount() { return amount; } public void setAmount(Double amount) { this.amount = amount; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public LocalDateTime getBillDate() { return billDate; } public void setBillDate(LocalDateTime billDate) { this.billDate = billDate; }
    public BillStatus getStatus() { return status; } public void setStatus(BillStatus status) { this.status = status; }
    public BigDecimal getSubtotal() { return subtotal; } public void setSubtotal(BigDecimal v) { this.subtotal = v; }
    public BigDecimal getDiscountAmount() { return discountAmount; } public void setDiscountAmount(BigDecimal v) { this.discountAmount = v; }
    public BigDecimal getTaxPercent() { return taxPercent; } public void setTaxPercent(BigDecimal v) { this.taxPercent = v; }
    public BigDecimal getTaxAmount() { return taxAmount; } public void setTaxAmount(BigDecimal v) { this.taxAmount = v; }
    public BigDecimal getTotalAmount() { return totalAmount; } public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }
    public BigDecimal getAmountPaid() { return amountPaid; } public void setAmountPaid(BigDecimal v) { this.amountPaid = v; }
    public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public Integer getRevision() { return revision; } public void setRevision(Integer v) { this.revision = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { this.createdBy = v; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String v) { this.updatedBy = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
