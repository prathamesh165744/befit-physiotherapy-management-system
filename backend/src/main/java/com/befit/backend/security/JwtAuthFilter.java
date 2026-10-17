package com.befit.backend.security;

import com.befit.backend.repository.UserRepository;
import com.befit.backend.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer <jwt>", loads the user from the database and puts it in the security context.
 * It never rejects a request itself: if the token is missing/bad the request simply stays anonymous and Spring
 * Security answers 401 (with the reason below) for protected URLs.
 *
 * Not a @Component on purpose - it is created inside SecurityConfig so it is registered once, not twice.
 */
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String ERROR_ATTRIBUTE = "befit.auth.error";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID = "TOKEN_INVALID";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String email = jwtUtil.parseSubject(header.substring(7).trim());
                var user = userRepository.findByEmail(email).filter(u -> u.isActive()).orElse(null);
                if (user == null) {
                    request.setAttribute(ERROR_ATTRIBUTE, TOKEN_INVALID);
                } else {
                    AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getBranchId());
                    var auth = new UsernamePasswordAuthenticationToken(principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(ERROR_ATTRIBUTE, TOKEN_EXPIRED);
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(ERROR_ATTRIBUTE, TOKEN_INVALID);
            }
        }
        chain.doFilter(request, response);
    }
}
