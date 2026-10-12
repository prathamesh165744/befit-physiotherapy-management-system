package com.befit.backend.controller;

import com.befit.backend.dto.PatientProfileUpdate;
import com.befit.backend.entity.Patient;
import com.befit.backend.repository.PatientRepository;
import com.befit.backend.service.PatientService;
import com.befit.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
public class PatientController {

    @Autowired private PatientRepository patientRepository;
    @Autowired private PatientService patientService;
    @Autowired private JwtUtil jwtUtil;

    // ---------- Staff ----------

    /** Default: patients currently at this branch. ?scope=all adds patients who moved away but still have visits/bills here. */
    @GetMapping
    public ResponseEntity<List<Patient>> getPatients(
            @RequestParam(required = false, defaultValue = "KARVE-ROAD") String branchId,
            @RequestParam(required = false, defaultValue = "current") String scope) {
        return ResponseEntity.ok("all".equals(scope)
                ? patientRepository.findCurrentOrPastByBranch(branchId)
                : patientRepository.findByBranchId(branchId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        return patientRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Patient> registerPatient(@RequestBody Patient patient) {
        if (patient.getBranchId() == null) patient.setBranchId("KARVE-ROAD");
        if (patient.getRegistrationDate() == null) patient.setRegistrationDate(LocalDate.now());
        return ResponseEntity.ok(patientRepository.save(patient));
    }

    // ---------- Logged-in patient (identified by the JWT, never by an id in the URL) ----------

    @GetMapping("/me")
    public ResponseEntity<?> myProfile(@RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            return ResponseEntity.ok(patientService.getPatientProfileByEmail(jwtUtil.extractEmail(auth)));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestHeader(value = "Authorization", required = false) String auth,
                                             @RequestBody PatientProfileUpdate update) {
        try {
            return ResponseEntity.ok(patientService.updatePatientProfile(jwtUtil.extractEmail(auth), update));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Body: { "branchId": "BANER" } */
    @PutMapping("/me/branch")
    public ResponseEntity<?> changeMyBranch(@RequestHeader(value = "Authorization", required = false) String auth,
                                            @RequestBody Map<String, String> body) {
        try {
            Patient p = patientService.changeBranch(jwtUtil.extractEmail(auth), body.get("branchId"));
            return ResponseEntity.ok(p);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
