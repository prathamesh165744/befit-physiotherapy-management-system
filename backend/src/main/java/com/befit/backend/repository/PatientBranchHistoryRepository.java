package com.befit.backend.repository;

import com.befit.backend.entity.PatientBranchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PatientBranchHistoryRepository extends JpaRepository<PatientBranchHistory, Long> {
    Optional<PatientBranchHistory> findByPatientIdAndToDateIsNull(Long patientId);
    List<PatientBranchHistory> findByPatientIdOrderByFromDateDesc(Long patientId);
}
