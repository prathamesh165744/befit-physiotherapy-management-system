package com.befit.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401 for "not logged in / token expired / token invalid" - as JSON the frontend can read. */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        Object reason = request.getAttribute(JwtAuthFilter.ERROR_ATTRIBUTE);
        String code = reason != null ? reason.toString() : "UNAUTHENTICATED";
        String message = JwtAuthFilter.TOKEN_EXPIRED.equals(code)
                ? "Your session has expired. Please log in again."
                : "Please log in to continue.";
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
