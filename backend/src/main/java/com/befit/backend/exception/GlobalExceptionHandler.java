package com.befit.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One place that turns exceptions into the right HTTP status.
 * 401 / 403 / 404 / 409 / 500 are kept separate so the frontend can react to each correctly.
 * Stack traces and SQL are only written to the server log, never sent to the browser.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> build(int status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, code, message));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e) {
        return build(e.getStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        String first = fields.values().stream().findFirst().orElse("Invalid request");
        return ResponseEntity.status(400).body(new ApiError(400, "VALIDATION_FAILED", first, fields, Instant.now()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleBadInput(Exception e) {
        return build(400, "BAD_REQUEST", "The request could not be understood. Please check the values and try again.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException e) {
        return build(403, "FORBIDDEN", "You do not have permission to do that.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException e) {
        return build(404, "NOT_FOUND", "Not found.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException e) {
        return build(405, "METHOD_NOT_ALLOWED", "This action is not supported.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return build(409, "CONFLICT", "That change conflicts with existing data (it may already exist).");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception e) {
        // Spring's own HTTP errors (415 wrong content type, 406, ...) keep their real status instead of becoming a 500
        if (e instanceof ErrorResponse er && er.getStatusCode().value() < 500) {
            int status = er.getStatusCode().value();
            return build(status, "HTTP_" + status, "The request could not be processed.");
        }
        log.error("Unhandled error", e);
        return build(500, "INTERNAL_ERROR", "Something went wrong on our side. Please try again.");
    }
}
