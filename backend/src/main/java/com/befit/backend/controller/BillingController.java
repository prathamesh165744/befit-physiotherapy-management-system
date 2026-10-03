package com.befit.backend.controller;

import com.befit.backend.entity.Appointment;
import com.befit.backend.entity.AppointmentStatus;
import com.befit.backend.entity.Bill;
import com.befit.backend.repository.AppointmentRepository;
import com.befit.backend.repository.BillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
@CrossOrigin(origins = "*")
public class BillingController {
    
    @Autowired private BillRepository billRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    @GetMapping
    public ResponseEntity<List<Bill>> getAllBills(@RequestParam(required = false, defaultValue = "KARVE-ROAD") String branchId) {
        return ResponseEntity.ok(billRepository.findByBranchIdOrderByBillDateDesc(branchId));
    }

    @PostMapping("/manual")
    public ResponseEntity<Bill> createManualBill(@RequestBody Bill bill) {
        if (bill.getBranchId() == null) bill.setBranchId("KARVE-ROAD");
        bill.setBillDate(LocalDateTime.now());
        bill.setStatus("PAID");
        return ResponseEntity.ok(billRepository.save(bill));
    }

    @PostMapping("/appointment/{id}")
    public ResponseEntity<Bill> generateAppointmentBill(@PathVariable Long id, @RequestBody Bill billRequest) {
        Appointment apt = appointmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Appointment not found"));
        
        apt.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(apt);

        Bill bill = new Bill();
        bill.setAppointmentId(apt.getId());
        
        // Safely extract patient name
        String patientName = apt.getPatient() != null ? apt.getPatient().getFullName() : "Walk-in Patient";
        bill.setPatientName(patientName);
        
        // Safely extract branch
        bill.setBranchId(apt.getBranchId() != null ? apt.getBranchId() : "KARVE-ROAD");
        bill.setAmount(billRequest.getAmount());
        bill.setDescription(billRequest.getDescription() != null ? billRequest.getDescription() : apt.getCaseType() + " Charges");
        bill.setBillDate(LocalDateTime.now());
        bill.setStatus("PAID");

        return ResponseEntity.ok(billRepository.save(bill));
    }
}