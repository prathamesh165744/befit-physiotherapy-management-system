package com.befit.backend.controller;

import com.befit.backend.dto.AuthResponse;
import com.befit.backend.dto.LoginRequest;
import com.befit.backend.dto.RegisterRequest;
import com.befit.backend.entity.User;
import com.befit.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@RequestBody RegisterRequest request) {
        try {
            User registeredUser = userService.registerPatient(request);
            return ResponseEntity.ok("Patient registered successfully! User ID: " + registeredUser.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Call the updated loginUser method which returns the AuthResponse object
            AuthResponse authResponse = userService.loginUser(request);
            
            // Return the full object (token, role, fullName) as JSON
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage());
        }
    }
}