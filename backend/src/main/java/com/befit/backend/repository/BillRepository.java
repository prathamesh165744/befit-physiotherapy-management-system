package com.befit.backend.repository;

import com.befit.backend.entity.Bill;
import com.befit.backend.entity.BillStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    /**
     * One filter shared by the paged list, "Print All" and the Excel export, so all three always agree.
     * No parameter is ever null (PostgreSQL cannot infer the type of a null parameter):
     * the service passes 1970 / 2999 for an open date range, "%" for no search text, -1 for "not an id".
     */
    String FILTER = "b.branchId in :branchIds and b.billDate >= :from and b.billDate < :to and b.status in :statuses " +
            "and (lower(b.patientName) like :q or lower(b.invoiceNumber) like :q or lower(b.description) like :q or b.patientId = :pid)";

    @Query(value = "select b from Bill b where " + FILTER, countQuery = "select count(b) from Bill b where " + FILTER)
    Page<Bill> search(@Param("branchIds") Collection<String> branchIds, @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to, @Param("statuses") Collection<BillStatus> statuses,
                      @Param("q") String q, @Param("pid") Long pid, Pageable pageable);

    /** Same filter, no count query - used with a row cap by the report and the export. */
    @Query("select b from Bill b where " + FILTER)
    List<Bill> searchAll(@Param("branchIds") Collection<String> branchIds, @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to, @Param("statuses") Collection<BillStatus> statuses,
                         @Param("q") String q, @Param("pid") Long pid, Pageable pageable);

    /** Row-locks the invoice so two receptionists cannot edit / pay it at the same instant. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") Long id);

    List<Bill> findByPatientIdAndBranchIdInOrderByBillDateDesc(Long patientId, Collection<String> branchIds, Pageable pageable);
    List<Bill> findByPatientIdOrderByBillDateDesc(Long patientId, Pageable pageable);

    boolean existsByPatientIdAndBranchId(Long patientId, String branchId);

    /** Transfer validation: the patient's invoices (any branch) in the given statuses, oldest first. Read-only use. */
    List<Bill> findByPatientIdAndStatusInOrderByBillDateAsc(Long patientId, Collection<BillStatus> statuses);

    boolean existsByAppointmentIdAndStatusNot(Long appointmentId, BillStatus status);

    // ---- startup backfill helpers (DataFixer) ----
    @Modifying
    @Query("update Bill b set b.branchId = :branchId where b.branchId is null")
    int fillNullBranch(@Param("branchId") String branchId);

    List<Bill> findByPatientIdIsNullAndAppointmentIdIsNotNull();
    List<Bill> findByTotalAmountIsNull(Pageable pageable);
    List<Bill> findByInvoiceNumberIsNull(Pageable pageable);
}
