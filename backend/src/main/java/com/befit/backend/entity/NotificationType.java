package com.befit.backend.entity;

/**
 * What happened. Every type is tied to one approved workflow event; add a value here (and a producer in NotificationService)
 * to introduce another branch workflow notification.
 */
public enum NotificationType {
    TRANSFER_REQUESTED,   // a branch transfer was requested  -> the people who can decide it
    TRANSFER_APPROVED,    // ... approved                      -> requester, patient, source-branch managers
    TRANSFER_REJECTED,    // ... rejected                      -> requester, patient, source-branch managers
    TRANSFER_CANCELLED    // ... withdrawn                     -> the people who can decide it, requester, patient
}
