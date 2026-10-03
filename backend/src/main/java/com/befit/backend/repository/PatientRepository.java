package com.befit.backend.repository;

import com.befit.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    List<Patient> findByBranchId(String branchId);
    
    // RESTORED: Allows PatientService to find a patient's medical record via their login email
    Optional<Patient> findByUserEmail(String email);
}