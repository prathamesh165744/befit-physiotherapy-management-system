package com.befit.backend.dto;

import com.befit.backend.entity.BillAuditLog;

import java.time.LocalDateTime;

public record BillAuditDTO(Long id, String action, String changedBy, LocalDateTime changedAt, String reason,
                           String previousValues, String newValues) {
    public static BillAuditDTO of(BillAuditLog a) {
        return new BillAuditDTO(a.getId(), a.getAction(), a.getChangedBy(), a.getChangedAt(), a.getReason(),
                a.getPreviousValues(), a.getNewValues());
    }
}
