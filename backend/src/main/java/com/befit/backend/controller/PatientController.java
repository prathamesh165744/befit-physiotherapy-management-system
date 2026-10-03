package com.befit.backend.controller;

import com.befit.backend.entity.Patient;
import com.befit.backend.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
public class PatientController {
    
    @Autowired 
    private PatientRepository patientRepository;

    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients(@RequestParam(required = false, defaultValue = "KARVE-ROAD") String branchId) {
        return ResponseEntity.ok(patientRepository.findByBranchId(branchId));
    }

    // NEW: Fetch a single patient by ID for their Profile Page
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        return patientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Patient> registerPatient(@RequestBody Patient patient) {
        if (patient.getBranchId() == null) {
            patient.setBranchId("KARVE-ROAD");
        }
        if (patient.getRegistrationDate() == null) {
            patient.setRegistrationDate(LocalDate.now());
        }
        return ResponseEntity.ok(patientRepository.save(patient));
    }
}