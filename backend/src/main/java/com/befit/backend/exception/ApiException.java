package com.befit.backend.exception;

/** A failure with a clean HTTP status and a message that is safe to show to the user. */
public class ApiException extends RuntimeException {
    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }

    public static ApiException badRequest(String msg) { return new ApiException(400, "BAD_REQUEST", msg); }
    public static ApiException forbidden(String msg) { return new ApiException(403, "FORBIDDEN", msg); }
    public static ApiException notFound(String msg) { return new ApiException(404, "NOT_FOUND", msg); }
    public static ApiException conflict(String code, String msg) { return new ApiException(409, code, msg); }
}
