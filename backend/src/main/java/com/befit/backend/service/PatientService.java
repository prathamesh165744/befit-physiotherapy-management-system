package com.befit.backend.service;

import com.befit.backend.dto.PatientProfileUpdate;
import com.befit.backend.entity.AppointmentStatus;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.PatientBranchHistory;
import com.befit.backend.repository.AppointmentRepository;
import com.befit.backend.repository.BranchRepository;
import com.befit.backend.repository.PatientBranchHistoryRepository;
import com.befit.backend.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PatientService {

    @Autowired private PatientRepository patientRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private PatientBranchHistoryRepository historyRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    public List<Patient> getAllPatients() { return patientRepository.findAll(); }

    public Patient getPatientProfileByEmail(String email) {
        return patientRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Patient profile not found for the authenticated user."));
    }

    @Transactional
    public Patient updatePatientProfile(String email, PatientProfileUpdate u) {
        Patient p = getPatientProfileByEmail(email);
        if (u.getFullName() != null) p.setFullName(u.getFullName());
        if (u.getPhone() != null) p.setPhone(u.getPhone());
        if (u.getGender() != null) p.setGender(u.getGender());
        if (u.getAddress() != null) p.setAddress(u.getAddress());
        if (u.getEmergencyContact() != null) p.setEmergencyContact(u.getEmergencyContact());
        if (u.getEmergencyContactName() != null) p.setEmergencyContactName(u.getEmergencyContactName());
        if (u.getBloodGroup() != null) p.setBloodGroup(u.getBloodGroup());
        if (u.getOccupation() != null) p.setOccupation(u.getOccupation());
        return patientRepository.save(p);
    }

    /**
     * Moves the patient to another branch.
     * Only patients.branch_id changes. Existing appointments and bills keep their own branch_id,
     * so the old branch keeps its billing and visit records untouched.
     */
    @Transactional
    public Patient changeBranch(String email, String newBranchId) {
        Patient p = getPatientProfileByEmail(email);

        var branch = branchRepository.findById(newBranchId)
                .orElseThrow(() -> new RuntimeException("Unknown branch: " + newBranchId));
        if (!branch.isActive()) throw new RuntimeException("This branch is not accepting patients right now.");
        if (newBranchId.equals(p.getBranchId())) return p;

        // Don't strand a booked visit at the old branch
        boolean hasOpenVisit = appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(p.getId()).stream()
                .anyMatch(a -> a.getStatus() == AppointmentStatus.SCHEDULED || a.getStatus() == AppointmentStatus.WAITING
                        || a.getStatus() == AppointmentStatus.CHECKED_IN || a.getStatus() == AppointmentStatus.IN_PROGRESS);
        if (hasOpenVisit) {
            throw new RuntimeException("You have an open appointment at your current branch. Complete or cancel it before switching.");
        }

        historyRepository.findByPatientIdAndToDateIsNull(p.getId()).ifPresent(h -> {
            h.setToDate(LocalDateTime.now());
            historyRepository.save(h);
        });
        historyRepository.save(new PatientBranchHistory(p.getId(), newBranchId));

        p.setBranchId(newBranchId);
        return patientRepository.save(p);
    }
}
