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
 * Authentication is now REAL: every URL except login/register and the public branch list needs a valid JWT,
 * and role rules are enforced here. (Before, this file contained anyRequest().permitAll().)
 * Branch rules (who may see which branch's rows) are enforced in the services via BranchAccess.
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUtil jwtUtil, UserRepository userRepository,
                                                   RestAuthEntryPoint entryPoint, RestAccessDeniedHandler deniedHandler) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // stateless API, token in header (no cookies) -> CSRF does not apply
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // browser pre-flight
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/branches").permitAll() // registration page needs the branch list
                .requestMatchers("/api/branches/**").hasRole("ADMIN")
                .requestMatchers("/api/patients/me", "/api/patients/me/**").hasRole("PATIENT")
                .requestMatchers("/api/transfers/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers("/api/billing/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers("/api/patients/**", "/api/appointments/**", "/api/dashboard/**", "/api/staff/**")
                    .hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthFilter(jwtUtil, userRepository), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition")); // lets the browser read the Excel file name
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
