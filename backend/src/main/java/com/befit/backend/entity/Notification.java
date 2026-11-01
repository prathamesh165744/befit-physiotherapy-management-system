package com.befit.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One notification for ONE user. recipientUserId is fixed when it is created; every read or update goes through that id,
 * so a user can only ever see or change their own. Unread = readAt is null.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_user_id, created_at")
})
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "recipient_user_id", nullable = false) private Long recipientUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private NotificationType type;
    @Column(nullable = false, length = 500) private String message;
    /** What the notification is about (e.g. "TRANSFER" + the transfer request id). The link is worked out per viewer, when read. */
    @Column(name = "related_type", length = 30) private String relatedType;
    @Column(name = "related_id") private Long relatedId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "read_at") private LocalDateTime readAt;

    public Notification() {}
    public Notification(Long recipientUserId, NotificationType type, String message, String relatedType, Long relatedId) {
        this.recipientUserId = recipientUserId; this.type = type; this.message = message;
        this.relatedType = relatedType; this.relatedId = relatedId;
    }

    public boolean isRead() { return readAt != null; }

    public Long getId() { return id; }
    public Long getRecipientUserId() { return recipientUserId; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public String getRelatedType() { return relatedType; }
    public Long getRelatedId() { return relatedId; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getReadAt() { return readAt; } public void setReadAt(LocalDateTime v) { this.readAt = v; }
}
