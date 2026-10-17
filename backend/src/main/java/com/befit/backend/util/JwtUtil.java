package com.befit.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Creates and validates login tokens.
 *
 * The signing key now comes from configuration (jwt.secret). Before, a NEW random key was generated every
 * time the server started, so every restart silently invalidated every logged-in user's token.
 */
@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final String DEV_SECRET_MARKER = "befit-dev-secret";

    private final Key key;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters long (set the JWT_SECRET environment variable).");
        }
        if (secret.startsWith(DEV_SECRET_MARKER)) {
            log.warn("Using the built-in DEVELOPMENT jwt.secret. Set JWT_SECRET before deploying anywhere real.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Returns the email inside the token.
     * Throws io.jsonwebtoken.ExpiredJwtException if expired, io.jsonwebtoken.JwtException if tampered/invalid.
     */
    public String parseSubject(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
}
