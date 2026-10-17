package com.befit.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Everything needed to print or edit ONE invoice. */
public record BillDetailDTO(BillDTO bill, List<Item> items, List<Pay> payments, BranchDTO branch,
                            PatientInfo patient, AppointmentInfo appointment,
                            boolean canEdit, boolean canPay, boolean canCancel, String lockReason) {

    public record Item(Long id, String description, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public record Pay(Long id, BigDecimal amount, String method, String status, LocalDateTime paymentDate,
                      String referenceNo, String receivedBy) {}

    /** Only what an invoice needs - no medical data. */
    public record PatientInfo(Long id, String regId, String fullName, String phone, String gender, String address) {}

    public record AppointmentInfo(Long id, LocalDateTime appointmentDate, String doctorName, String caseType) {}
}
