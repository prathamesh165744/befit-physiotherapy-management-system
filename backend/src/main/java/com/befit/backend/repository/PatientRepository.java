package com.befit.backend.repository;

import com.befit.backend.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    String SEARCH = "(lower(p.fullName) like :q or p.phone like :q or cast(p.id as string) like :q)";

    /** Patients whose CURRENT branch is one of branchIds. q is a ready-made LIKE pattern ("%abc%", never null). */
    @Query(value = "select p from Patient p where p.branchId in :branchIds and " + SEARCH,
           countQuery = "select count(p) from Patient p where p.branchId in :branchIds and " + SEARCH)
    Page<Patient> searchCurrent(@Param("branchIds") Collection<String> branchIds, @Param("q") String q, Pageable pageable);

    /** Current patients PLUS patients who moved away but still have visits/invoices at these branches. */
    @Query(value = "select p from Patient p where (p.branchId in :branchIds " +
                   "or exists (select 1 from Appointment a where a.patient = p and a.branchId in :branchIds) " +
                   "or exists (select 1 from Bill b where b.patientId = p.id and b.branchId in :branchIds)) and " + SEARCH,
           countQuery = "select count(p) from Patient p where (p.branchId in :branchIds " +
                   "or exists (select 1 from Appointment a where a.patient = p and a.branchId in :branchIds) " +
                   "or exists (select 1 from Bill b where b.patientId = p.id and b.branchId in :branchIds)) and " + SEARCH)
    Page<Patient> searchCurrentOrPast(@Param("branchIds") Collection<String> branchIds, @Param("q") String q, Pageable pageable);

    Optional<Patient> findByUserEmail(String email);

    // ---- startup backfill helpers (DataFixer) ----
    @Modifying
    @Query("update Patient p set p.branchId = :branchId where p.branchId is null")
    int fillNullBranch(@Param("branchId") String branchId);

    @Query("select p from Patient p where not exists " +
           "(select 1 from PatientBranchHistory h where h.patientId = p.id and h.toDate is null)")
    List<Patient> findWithoutCurrentBranchHistory();
}
