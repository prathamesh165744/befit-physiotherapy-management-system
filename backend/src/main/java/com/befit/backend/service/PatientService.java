package com.befit.backend.service;

import com.befit.backend.entity.Patient;
import com.befit.backend.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    // For Staff: Get all patients
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    // For Patient: Get their own profile securely via JWT email
    public Patient getPatientProfileByEmail(String email) {
        return patientRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Patient profile not found for the authenticated user."));
    }

    // For Patient: Update their own profile
    public Patient updatePatientProfile(String email, Patient updatedData) {
        Patient existingPatient = getPatientProfileByEmail(email);
        
        // Update only allowed fields (Security validation)
        if (updatedData.getFullName() != null) existingPatient.setFullName(updatedData.getFullName());
        if (updatedData.getPhone() != null) existingPatient.setPhone(updatedData.getPhone());
        if (updatedData.getEmergencyContact() != null) existingPatient.setEmergencyContact(updatedData.getEmergencyContact());
        if (updatedData.getGender() != null) existingPatient.setGender(updatedData.getGender());
        
        return patientRepository.save(existingPatient);
    }
}