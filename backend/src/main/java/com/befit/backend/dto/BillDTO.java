package com.befit.backend.dto;

import com.befit.backend.entity.Bill;
import com.befit.backend.entity.BillStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Invoice as shown in the billing list, the Excel export and the bulk report.
 * "amount" is an alias of totalAmount kept so old frontend code reading b.amount still works.
 */
public record BillDTO(Long id, String invoiceNumber, Long patientId, String patientName, String branchId, String branchName,
                      Long appointmentId, LocalDateTime billDate, String description,
                      BigDecimal subtotal, BigDecimal discountAmount, BigDecimal taxPercent, BigDecimal taxAmount,
                      BigDecimal totalAmount, BigDecimal amountPaid, BigDecimal balanceDue,
                      String status, String paymentMethod, int revision, BigDecimal amount) {

    /** Null-safe: rows created before the multi-branch upgrade may have empty money columns until DataFixer runs. */
    public static BillDTO of(Bill b, String branchName) {
        BigDecimal legacy = b.getAmount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(b.getAmount());
        BigDecimal total = scale(b.getTotalAmount() != null ? b.getTotalAmount() : legacy);
        BigDecimal subtotal = scale(b.getSubtotal() != null ? b.getSubtotal() : total);
        BigDecimal paid = b.getAmountPaid() != null ? scale(b.getAmountPaid())
                : (b.getStatus() == BillStatus.PAID ? total : scale(BigDecimal.ZERO));
        BigDecimal balance = b.getStatus() == BillStatus.CANCELLED ? scale(BigDecimal.ZERO) : scale(total.subtract(paid).max(BigDecimal.ZERO));
        String invoiceNo = b.getInvoiceNumber() != null ? b.getInvoiceNumber() : "INV-" + b.getId();
        return new BillDTO(b.getId(), invoiceNo, b.getPatientId(), b.getPatientName(), b.getBranchId(), branchName,
                b.getAppointmentId(), b.getBillDate(), b.getDescription(),
                subtotal, scale(b.getDiscountAmount()), scale(b.getTaxPercent()), scale(b.getTaxAmount()),
                total, paid, balance,
                b.getStatus() == null ? "PAID" : b.getStatus().name(),
                b.getPaymentMethod(), b.getRevision() == null ? 0 : b.getRevision(), total);
    }

    private static BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
