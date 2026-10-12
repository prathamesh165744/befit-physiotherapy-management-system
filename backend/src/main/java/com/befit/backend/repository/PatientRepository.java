package com.befit.backend.repository;

import com.befit.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** Patients whose CURRENT branch is this one. */
    List<Patient> findByBranchId(String branchId);

    /**
     * Current patients of the branch PLUS patients who moved away but still have appointments here
     * (so the old branch can still open their history and bills).
     */
    @Query("select p from Patient p where p.branchId = :branchId " +
           "or exists (select 1 from Appointment a where a.patient = p and a.branchId = :branchId)")
    List<Patient> findCurrentOrPastByBranch(@Param("branchId") String branchId);

    Optional<Patient> findByUserEmail(String email);
}
