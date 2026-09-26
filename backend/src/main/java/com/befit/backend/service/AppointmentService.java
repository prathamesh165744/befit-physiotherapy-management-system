package com.befit.backend.service;
import com.befit.backend.entity.Appointment;
import com.befit.backend.entity.AppointmentStatus;
import com.befit.backend.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AppointmentService {
    @Autowired private AppointmentRepository appointmentRepository;
    public List<Appointment> getAllAppointments() { return appointmentRepository.findAll(); }
    public Appointment updateAppointmentStatus(Long id, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Appointment not found"));
        appointment.setStatus(newStatus);
        return appointmentRepository.save(appointment);
    }
}