package com.befit.backend.dto;

import java.time.LocalDateTime;

/**
 * Returned ONCE to the staff member who creates an invitation. token is the secret (only its hash is stored, so it can
 * never be shown again); activationPath is the front-end path that carries it in the URL fragment, which browsers never
 * send to servers or put in access logs.
 */
public record PortalInvitationDTO(String token, String activationPath, LocalDateTime expiresAt) {}
