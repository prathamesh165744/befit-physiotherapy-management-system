package com.befit.backend.dto;

/** Body of POST /api/auth/invitations/check (request) and its answer. Reveals nothing but validity and the first name. */
public record InvitationCheckDTO(boolean valid, String firstName) {
    public record Request(String token) {}
}
