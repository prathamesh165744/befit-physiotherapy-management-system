package com.befit.backend.controller;

import com.befit.backend.dto.AuthResponse;
import com.befit.backend.dto.LoginRequest;
import com.befit.backend.dto.RegisterRequest;
import com.befit.backend.entity.User;
import com.befit.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody RegisterRequest request) {
        User registeredUser = userService.registerPatient(request);
        return ResponseEntity.ok(Map.of("message", "Patient registered successfully!", "userId", registeredUser.getId()));
    }

    /** Success body is unchanged: { token, role, fullName, branchId }. Failures are 401 { code, message }. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.loginUser(request));
    }
}
