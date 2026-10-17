package com.befit.backend.dto;

import com.befit.backend.entity.BranchTransferRequest;

import java.time.LocalDateTime;

public record TransferDTO(Long id, Long patientId, String patientName, String fromBranchId, String fromBranchName,
                          String toBranchId, String toBranchName, String status, String reason, String requestedBy,
                          LocalDateTime requestedAt, String decidedBy, LocalDateTime decidedAt, String decisionNote) {
    public static TransferDTO of(BranchTransferRequest t, String patientName, String fromName, String toName) {
        return new TransferDTO(t.getId(), t.getPatientId(), patientName, t.getFromBranchId(), fromName, t.getToBranchId(), toName,
                t.getStatus().name(), t.getReason(), t.getRequestedBy(), t.getRequestedAt(), t.getDecidedBy(), t.getDecidedAt(), t.getDecisionNote());
    }
}
