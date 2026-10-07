package com.befit.backend.config;

import com.befit.backend.entity.*;
import com.befit.backend.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataFixer {
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private BillRepository billRepository;
    @Autowired private UserRepository userRepository;

    @PostConstruct
    public void fixOldData() {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            if (u.getBranchId() == null) {
                u.setBranchId("KARVE-ROAD");
                userRepository.save(u);
            }
        }
        List<Patient> patients = patientRepository.findAll();
        for (Patient p : patients) {
            if (p.getBranchId() == null) {
                p.setBranchId("KARVE-ROAD");
                patientRepository.save(p);
            }
        }
        List<Appointment> apts = appointmentRepository.findAll();
        for (Appointment a : apts) {
            if (a.getBranchId() == null) {
                a.setBranchId("KARVE-ROAD");
                appointmentRepository.save(a);
            }
        }
        List<Bill> bills = billRepository.findAll();
        for (Bill b : bills) {
            if (b.getBranchId() == null) {
                b.setBranchId("KARVE-ROAD");
                billRepository.save(b);
            }
        }

        System.out.println("✅ ALL USERS AND DATA SYNCED TO KARVE-ROAD BRANCH");
        
    }
}