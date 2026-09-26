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

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {
    
    @Autowired 
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository; // Added to fetch the patient

    @GetMapping
    public ResponseEntity<List<Appointment>> getAllAppointments(@RequestParam(required = false, defaultValue = "KARVE-ROAD") String branchId) {
        return ResponseEntity.ok(appointmentRepository.findByBranchId(branchId));
    }

    // Inner DTO class to perfectly match the JSON sent from your React frontend
    public static class AppointmentPayload {
        public Long patientId;
        public Long doctorId;
        public String doctorName;
        public String appointmentDate;
        public String appointmentTime;
        public String caseType;
        public String branchId;
    }

    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody AppointmentPayload payload) {
        try {
            // 1. Fetch the actual Patient from the database using the ID from React
            Patient patient = patientRepository.findById(payload.patientId)
                    .orElseThrow(() -> new RuntimeException("Patient ID " + payload.patientId + " not found"));

            // 2. Build the new Appointment and attach the Patient record
            Appointment appointment = new Appointment();
            appointment.setPatient(patient); 
            appointment.setDoctorId(payload.doctorId);
            appointment.setDoctorName(payload.doctorName);
            appointment.setCaseType(payload.caseType);
            appointment.setAppointmentDate(LocalDateTime.parse(payload.appointmentDate));
            
            appointment.setBranchId(payload.branchId != null ? payload.branchId : "KARVE-ROAD");
            appointment.setStatus(AppointmentStatus.WAITING);
            appointment.setCheckInTime(LocalDateTime.now()); 
            
            // 3. Save to database
            return ResponseEntity.ok(appointmentRepository.save(appointment));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to book appointment: " + e.getMessage());
        }
    }
}