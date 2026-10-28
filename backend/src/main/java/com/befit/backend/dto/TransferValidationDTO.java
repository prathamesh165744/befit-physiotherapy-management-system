package com.befit.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * What the approver must see before deciding on a transfer.
 *  blockers : hard clinic rules - the transfer cannot be approved until they are fixed.
 *  warnings : the approver may proceed only after acknowledging them (and writing a note).
 *
 * Billing part (only ever sent to staff allowed to decide / view the transfer, never to patients):
 *  outstandingInvoices / outstandingAmount cover ALL unpaid and partly paid invoices of the patient at every branch;
 *  dueInvoices lists up to 50 of them (oldest first), dueInvoicesNotShown is the rest.
 *  outstandingRule = BLOCK (transfer refused while money is due) or WARN_AND_DOCUMENT (allowed after acknowledgement + note).
 */
public record TransferValidationDTO(Long transferId, String status, boolean canApprove, boolean requiresAcknowledgement,
                                    List<Issue> blockers, List<Issue> warnings,
                                    long openAppointments, long futureAppointments,
                                    long outstandingInvoices, BigDecimal outstandingAmount,
                                    String outstandingRule, List<DueInvoice> dueInvoices, long dueInvoicesNotShown) {
    public record Issue(String code, String message) {}

    /** One invoice that still has money due. branchId/branchName = the branch that issued it and keeps collecting it. */
    public record DueInvoice(Long id, String invoiceNumber, String branchId, String branchName, LocalDateTime billDate,
                             String status, BigDecimal totalAmount, BigDecimal amountPaid, BigDecimal balanceDue) {}
}
