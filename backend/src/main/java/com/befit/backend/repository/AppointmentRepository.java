package com.befit.backend.repository;

import com.befit.backend.entity.Appointment;
import com.befit.backend.entity.AppointmentStatus;
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
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    String FILTER = "a.branchId in :branchIds and a.appointmentDate >= :from and a.appointmentDate < :to and a.status in :statuses";

    /** Paged list; the patient is fetched in the same query (no N+1). from is inclusive, to is exclusive. */
    @Query(value = "select a from Appointment a join fetch a.patient where " + FILTER,
           countQuery = "select count(a) from Appointment a where " + FILTER)
    Page<Appointment> search(@Param("branchIds") Collection<String> branchIds,
                             @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                             @Param("statuses") Collection<AppointmentStatus> statuses, Pageable pageable);

    /** One day on the receptionist dashboard. */
    @Query("select a from Appointment a join fetch a.patient where a.branchId in :branchIds " +
           "and a.appointmentDate >= :start and a.appointmentDate <= :end order by a.appointmentDate asc")
    List<Appointment> findDay(@Param("branchIds") Collection<String> branchIds,
                              @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);

    List<Appointment> findByPatientIdAndBranchIdInOrderByAppointmentDateDesc(Long patientId, Collection<String> branchIds, Pageable pageable);
    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Long patientId, Pageable pageable);

    boolean existsByPatientIdAndBranchId(Long patientId, String branchId);

    /**
     * Temporary-visit check: does the patient have a live visit at this branch? Live = still open (statuses given),
     * or finished (finishedStatus) no longer ago than {@code since}, so the desk can still close out the visit.
     */
    @Query("select count(a) > 0 from Appointment a where a.patient.id = :patientId and a.branchId = :branchId " +
           "and (a.status in :openStatuses or (a.status = :finishedStatus and a.appointmentDate >= :since))")
    boolean existsLiveVisit(@Param("patientId") Long patientId, @Param("branchId") String branchId,
                            @Param("openStatuses") Collection<AppointmentStatus> openStatuses,
                            @Param("finishedStatus") AppointmentStatus finishedStatus,
                            @Param("since") LocalDateTime since);
    boolean existsByPatientIdAndStatusIn(Long patientId, Collection<AppointmentStatus> statuses);

    /** Transfer validation: open visits due before {@code before} (today or overdue) / from {@code from} on (future). All branches. */
    long countByPatientIdAndStatusInAndAppointmentDateBefore(Long patientId, Collection<AppointmentStatus> statuses, LocalDateTime before);
    long countByPatientIdAndStatusInAndAppointmentDateGreaterThanEqual(Long patientId, Collection<AppointmentStatus> statuses, LocalDateTime from);
    /** Is the PATIENT already booked at this exact time, at any branch? (no double-booking across branches) */
    boolean existsByPatientIdAndAppointmentDateAndStatusNotIn(Long patientId, LocalDateTime date, Collection<AppointmentStatus> statuses);
    boolean existsByDoctorIdAndAppointmentDateAndStatusNotIn(Long doctorId, LocalDateTime date, Collection<AppointmentStatus> statuses);

    @Modifying
    @Query("update Appointment a set a.branchId = :branchId where a.branchId is null")
    int fillNullBranch(@Param("branchId") String branchId);
}
