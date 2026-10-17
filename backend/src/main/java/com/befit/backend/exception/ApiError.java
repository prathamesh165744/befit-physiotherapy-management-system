package com.befit.backend.exception;

import java.time.Instant;
import java.util.Map;

/** JSON body of every error response: { status, code, message, fieldErrors?, timestamp }. */
public record ApiError(int status, String code, String message, Map<String, String> fieldErrors, Instant timestamp) {
    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, null, Instant.now());
    }
}
