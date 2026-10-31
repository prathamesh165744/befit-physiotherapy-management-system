package com.befit.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A one-time invitation for a patient (registered by staff, no login yet) to create their portal account.
 * Only the SHA-256 hash of the secret token is stored: the token itself exists once, on the staff screen that created it,
 * so a database leak cannot be used to take over a patient's account.
 */
@Entity
@Table(name = "patient_invitations", indexes = {
        @Index(name = "idx_invitation_patient", columnList = "patient_id"),
        @Index(name = "idx_invitation_hash", columnList = "token_hash", unique = true)
})
public class PatientInvitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "patient_id", nullable = false) private Long patientId;
    @Column(name = "token_hash", nullable = false, length = 64, unique = true) private String tokenHash;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "used_at") private LocalDateTime usedAt;
    @Column(name = "revoked_at") private LocalDateTime revokedAt;

    public PatientInvitation() {}
    public PatientInvitation(Long patientId, String tokenHash, String createdBy, LocalDateTime expiresAt) {
        this.patientId = patientId; this.tokenHash = tokenHash; this.createdBy = createdBy; this.expiresAt = expiresAt;
    }

    /** Still usable: not used, not revoked, not expired. */
    public boolean isUsable(LocalDateTime now) { return usedAt == null && revokedAt == null && expiresAt.isAfter(now); }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public String getTokenHash() { return tokenHash; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getExpiresAt() { return expiresAt; } public void setExpiresAt(LocalDateTime v) { this.expiresAt = v; }
    public LocalDateTime getUsedAt() { return usedAt; } public void setUsedAt(LocalDateTime v) { this.usedAt = v; }
    public LocalDateTime getRevokedAt() { return revokedAt; } public void setRevokedAt(LocalDateTime v) { this.revokedAt = v; }
}
