package com.befit.backend.repository;
import com.befit.backend.entity.Appointment;
import com.befit.backend.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByBranchId(String branchId);
    List<Appointment> findByBranchIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(String branchId, LocalDateTime start, LocalDateTime end);
    List<Appointment> findTop5ByBranchIdAndStatusInOrderByCheckInTimeDesc(String branchId, List<AppointmentStatus> statuses);
}