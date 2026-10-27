package com.befit.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the approver must see before deciding on a transfer.
 *  blockers : hard clinic rules - the transfer cannot be approved until they are fixed.
 *  warnings : the approver may proceed only after acknowledging them (and writing a note).
 */
public record TransferValidationDTO(Long transferId, String status, boolean canApprove, boolean requiresAcknowledgement,
                                    List<Issue> blockers, List<Issue> warnings,
                                    long openAppointments, long futureAppointments,
                                    long outstandingInvoices, BigDecimal outstandingAmount) {
    public record Issue(String code, String message) {}
}
