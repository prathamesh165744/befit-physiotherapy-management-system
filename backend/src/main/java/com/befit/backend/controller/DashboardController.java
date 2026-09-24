package com.befit.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    // Only PATIENT can access this
    @GetMapping("/patient-data")
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<String> getPatientData() {
        return ResponseEntity.ok("This is protected Patient data.");
    }

    // Only ADMIN, DOCTOR, or RECEPTIONIST can access this
    @GetMapping("/staff-data")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<String> getStaffData() {
        return ResponseEntity.ok("This is protected Staff data.");
    }
}