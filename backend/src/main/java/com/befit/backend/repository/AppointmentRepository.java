package com.befit.backend.repository;

import com.befit.backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByBranchId(String branchId);
    List<Appointment> findByBranchIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(String branchId, LocalDateTime start, LocalDateTime end);
    
    // NEW: Fetch all past appointments for a specific patient's timeline
    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Long patientId);
}