package com.befit.backend.dto;

import com.befit.backend.entity.Notification;

import java.time.LocalDateTime;

/**
 * A notification as the RECIPIENT sees it. link is the front-end page to open, worked out for this viewer at read time:
 * it is null when the viewer may not open the related workflow (so the UI shows plain text, never a dead or forbidden link).
 */
public record NotificationDTO(Long id, String type, String message, LocalDateTime createdAt, boolean read, LocalDateTime readAt,
                              String relatedType, Long relatedId, String link) {
    public static NotificationDTO of(Notification n, String link) {
        return new NotificationDTO(n.getId(), n.getType().name(), n.getMessage(), n.getCreatedAt(), n.isRead(), n.getReadAt(),
                n.getRelatedType(), n.getRelatedId(), link);
    }
}
