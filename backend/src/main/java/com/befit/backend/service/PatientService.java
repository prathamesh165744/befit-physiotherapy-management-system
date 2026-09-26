package com.befit.backend.service;
import com.befit.backend.entity.Patient;
import com.befit.backend.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PatientService {
    @Autowired private PatientRepository patientRepository;
    public List<Patient> getAllPatients() { return patientRepository.findAll(); }
    public Patient getPatientById(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
    }
}