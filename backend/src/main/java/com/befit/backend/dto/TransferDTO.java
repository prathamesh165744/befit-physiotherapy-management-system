package com.befit.backend.dto;

import com.befit.backend.entity.BranchTransferRequest;

import java.time.LocalDateTime;

public record TransferDTO(Long id, Long patientId, String patientName, String fromBranchId, String fromBranchName,
                          String toBranchId, String toBranchName, String status, String reason, String requestedBy,
                          LocalDateTime requestedAt, String decidedBy, LocalDateTime decidedAt, String decisionNote,
                          String requestedByRole, String decisionWarnings,
                          java.math.BigDecimal outstandingAmount, Integer outstandingInvoices, String outstandingInvoiceRefs) {
    public static TransferDTO of(BranchTransferRequest t, String patientName, String fromName, String toName) {
        return new TransferDTO(t.getId(), t.getPatientId(), patientName, t.getFromBranchId(), fromName, t.getToBranchId(), toName,
                t.getStatus().name(), t.getReason(), t.getRequestedBy(), t.getRequestedAt(), t.getDecidedBy(), t.getDecidedAt(), t.getDecisionNote(),
                t.getRequestedByRole(), t.getDecisionWarnings(),
                t.getOutstandingAmount(), t.getOutstandingInvoices(), t.getOutstandingInvoiceRefs());
    }

    /** Copy without billing details - for patients and profile screens, who must not receive the approver's financial snapshot. */
    public TransferDTO withoutFinancials() {
        return new TransferDTO(id, patientId, patientName, fromBranchId, fromBranchName, toBranchId, toBranchName, status, reason,
                requestedBy, requestedAt, decidedBy, decidedAt, decisionNote, requestedByRole, null, null, null, null);
    }

    /**
     * The outgoing-tracking view for the SOURCE branch: who asked, where to, why, when, the status, who decided, when and
     * their note. What the destination's approver reviewed internally (acknowledged warnings, the invoice snapshot) is left out.
     */
    public TransferDTO forSourceBranch() {
        return new TransferDTO(id, patientId, patientName, fromBranchId, fromBranchName, toBranchId, toBranchName, status, reason,
                requestedBy, requestedAt, decidedBy, decidedAt, decisionNote, requestedByRole, null, null, null, null);
    }

    /** What a PATIENT may see of their own request: no financial snapshot and no staff e-mail addresses (decision time and note stay). */
    public TransferDTO forPatient() {
        return new TransferDTO(id, patientId, patientName, fromBranchId, fromBranchName, toBranchId, toBranchName, status, reason,
                requestedBy, requestedAt, null, decidedAt, decisionNote, requestedByRole, null, null, null, null);
    }
}
