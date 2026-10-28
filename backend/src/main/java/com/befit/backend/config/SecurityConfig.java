
package com.befit.backend.config;

import com.befit.backend.repository.UserRepository;
import com.befit.backend.security.JwtAuthFilter;
import com.befit.backend.security.RestAccessDeniedHandler;
import com.befit.backend.security.RestAuthEntryPoint;
import com.befit.backend.util.JwtUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Authentication and role-based authorization configuration.
 * Branch-level data access is enforced in services using BranchAccess.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${befit.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtUtil jwtUtil,
            UserRepository userRepository,
            RestAuthEntryPoint entryPoint,
            RestAccessDeniedHandler deniedHandler
    ) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // Allow browser CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public authentication endpoints
                .requestMatchers("/api/auth/**").permitAll()

                // Public branch list for registration
                .requestMatchers(HttpMethod.GET, "/api/branches").permitAll()

                // Only administrators can manage branches
                .requestMatchers("/api/branches/**").hasRole("ADMIN")

                // Patients can access their own profile endpoints
                .requestMatchers(
                    "/api/patients/me",
                    "/api/patients/me/**"
                ).hasRole("PATIENT")

                // Only admins and receptionists can search patients
                // across branches; doctors and patients receive 403
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/patients/cross-branch-search"
                ).hasAnyRole("ADMIN", "RECEPTIONIST")

                // Creating a branch manager account: administrators only
                .requestMatchers(HttpMethod.POST, "/api/staff/managers").hasRole("ADMIN")

                // Transfers: admins, branch managers (own branch) and receptionists (raise / view / withdraw only
                // while manager approval is required). Who may DECIDE is enforced in TransferService.
                .requestMatchers("/api/transfers/**")
                    .hasAnyRole("ADMIN", "BRANCH_MANAGER", "RECEPTIONIST")

                // Only admins and receptionists can access billing
                .requestMatchers("/api/billing/**")
                    .hasAnyRole("ADMIN", "RECEPTIONIST")

                // Staff-facing management endpoints (BRANCH_MANAGER is deliberately NOT here: patients, appointments,
                // dashboard and doctor lists stay with front-desk / clinical staff and admins)
                .requestMatchers(
                    "/api/patients/**",
                    "/api/appointments/**",
                    "/api/dashboard/**",
                    "/api/staff/**"
                ).hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")

                // All remaining endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthFilter(jwtUtil, userRepository),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
