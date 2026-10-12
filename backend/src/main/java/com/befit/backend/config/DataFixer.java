package com.befit.backend.config;

import com.befit.backend.entity.*;
import com.befit.backend.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/** One-time backfill for rows created before multi-branch support. Safe to run repeatedly. */
@Component
public class DataFixer {
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private BillRepository billRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientBranchHistoryRepository historyRepository;

    @PostConstruct
    public void fixOldData() {
        // Staff only: doctors / receptionists default to KARVE-ROAD. Admins stay null (= all branches).
        // Patients no longer use users.branch_id.
        for (User u : userRepository.findAll()) {
            if (u.getBranchId() == null && (u.getRole() == Role.DOCTOR || u.getRole() == Role.RECEPTIONIST)) {
                u.setBranchId("KARVE-ROAD");
                userRepository.save(u);
            }
        }
        for (Patient p : patientRepository.findAll()) {
            if (p.getBranchId() == null) { p.setBranchId("KARVE-ROAD"); patientRepository.save(p); }
            if (historyRepository.findByPatientIdAndToDateIsNull(p.getId()).isEmpty()) {
                historyRepository.save(new PatientBranchHistory(p.getId(), p.getBranchId()));
            }
        }
        for (Appointment a : appointmentRepository.findAll()) {
            if (a.getBranchId() == null) { a.setBranchId("KARVE-ROAD"); appointmentRepository.save(a); }
        }
        for (Bill b : billRepository.findAll()) {
            boolean changed = false;
            if (b.getBranchId() == null) { b.setBranchId("KARVE-ROAD"); changed = true; }
            if (b.getPatientId() == null && b.getAppointmentId() != null) {
                var apt = appointmentRepository.findById(b.getAppointmentId()).orElse(null);
                if (apt != null && apt.getPatient() != null) { b.setPatientId(apt.getPatient().getId()); changed = true; }
            }
            if (changed) billRepository.save(b);
        }
        System.out.println("✅ Multi-branch data backfill complete");
    }
}
