package com.befit.backend.controller;
import com.befit.backend.entity.Appointment;
import com.befit.backend.entity.AppointmentStatus;
import com.befit.backend.entity.Patient;
import com.befit.backend.repository.AppointmentRepository;
import com.befit.backend.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PatientRepository patientRepository;

    @GetMapping
    public ResponseEntity<List<Appointment>> getAllAppointments(@RequestParam(required = false, defaultValue = "KARVE-ROAD") String branchId) {
        return ResponseEntity.ok(appointmentRepository.findByBranchId(branchId));
    }

    public static class AppointmentPayload {
        public Long patientId; public Long doctorId; public String doctorName;
        public String appointmentDate; public String appointmentTime; public String caseType; public String branchId;
    }

    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody AppointmentPayload payload) {
        try {
            Patient patient = patientRepository.findById(payload.patientId).orElseThrow(() -> new RuntimeException("Patient not found"));
            Appointment appointment = new Appointment();
            appointment.setPatient(patient); 
            appointment.setDoctorId(payload.doctorId);
            appointment.setDoctorName(payload.doctorName);
            appointment.setCaseType(payload.caseType);
            appointment.setAppointmentDate(LocalDateTime.parse(payload.appointmentDate));
            appointment.setBranchId(payload.branchId != null ? payload.branchId : "KARVE-ROAD");
            appointment.setStatus(AppointmentStatus.WAITING);
            appointment.setCheckInTime(LocalDateTime.now()); 
            return ResponseEntity.ok(appointmentRepository.save(appointment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // NEW: API TO UPDATE STATUS (CHECK-IN / BILL)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            Appointment appointment = appointmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Appointment not found"));
            AppointmentStatus newStatus = AppointmentStatus.valueOf(payload.get("status").toUpperCase());
            appointment.setStatus(newStatus);
            if(newStatus == AppointmentStatus.CHECKED_IN) {
                appointment.setCheckInTime(LocalDateTime.now());
            }
            return ResponseEntity.ok(appointmentRepository.save(appointment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update status");
        }
    }

    // NEW: API TO FETCH A PATIENT'S APPOINTMENT HISTORY
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getPatientAppointments(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId));
    }
}