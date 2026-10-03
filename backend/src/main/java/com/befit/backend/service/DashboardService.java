package com.befit.backend.service;

import com.befit.backend.dto.DashboardDTO;
import com.befit.backend.entity.Appointment;
import com.befit.backend.entity.Patient;
import com.befit.backend.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    @Autowired private AppointmentRepository appointmentRepository;

    public DashboardDTO getReceptionistDashboardData(String branchId, String dateStr) {
        DashboardDTO dto = new DashboardDTO();
        String targetBranch = (branchId != null && !branchId.isEmpty()) ? branchId : "KARVE-ROAD";
        
        LocalDate targetDate = LocalDate.now();
        try {
            if (dateStr != null && !dateStr.isEmpty() && !dateStr.equals("undefined")) {
                targetDate = LocalDate.parse(dateStr);
            }
        } catch (Exception e) {
            System.out.println("Invalid date received, defaulting to today.");
        }
        
        LocalDateTime startOfDay = LocalDateTime.of(targetDate, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(targetDate, LocalTime.MAX);
        
        List<Appointment> dayAppointments = appointmentRepository.findByBranchIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(targetBranch, startOfDay, endOfDay);

        int checkedInCount = 0; int waitingCount = 0; long totalWaitMinutes = 0;
        List<Map<String, Object>> upcomingList = new ArrayList<>();
        List<Map<String, Object>> registrationList = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (Appointment apt : dayAppointments) {
            String status = apt.getStatus() != null ? apt.getStatus().name() : "WAITING";
            
            if (status.equals("CHECKED_IN")) checkedInCount++;
            if (status.equals("WAITING")) {
                waitingCount++;
                if (apt.getCheckInTime() != null && targetDate.equals(LocalDate.now())) {
                    totalWaitMinutes += Math.max(0, Duration.between(apt.getCheckInTime(), LocalDateTime.now()).toMinutes());
                }
            }

            Patient p = apt.getPatient();
            String pName = p != null && p.getFullName() != null ? p.getFullName() : "Walk-in Patient";
            String pAgeGender = p != null && p.getGender() != null ? "N/A / " + p.getGender() : "N/A / M";
            String pId = p != null ? "REG-" + p.getId() : "REG-000";
            String pIni = pName.length() >= 2 ? pName.substring(0, 2).toUpperCase() : "PT";

            Map<String, Object> upcomingMap = new HashMap<>();
            upcomingMap.put("id", apt.getId());
            upcomingMap.put("time", apt.getAppointmentDate() != null ? apt.getAppointmentDate().format(timeFormatter) : "10:00 AM");
            upcomingMap.put("name", pName);
            upcomingMap.put("doc", apt.getDoctorName() != null ? apt.getDoctorName() : "Doctor");
            upcomingMap.put("type", apt.getCaseType() != null ? apt.getCaseType() : "Consultation");
            upcomingMap.put("status", status.replace("_", " "));
            upcomingMap.put("color", (status.equals("WAITING") || status.equals("DELAYED")) ? "text-red-600 bg-red-50" : "text-[#2563eb] bg-[#eef2fc]");
            upcomingList.add(upcomingMap);

            Map<String, Object> regMap = new HashMap<>();
            regMap.put("id", apt.getId()); 
            regMap.put("regId", pId);
            regMap.put("ini", pIni);
            regMap.put("name", pName);
            regMap.put("age", pAgeGender);
            regMap.put("doc", apt.getDoctorName() != null ? apt.getDoctorName() : "Doctor");
            regMap.put("case", apt.getCaseType() != null ? apt.getCaseType() : "Consultation");
            regMap.put("status", status); 
            regMap.put("statusLabel", status.replace("_", " ")); 
            regMap.put("color", status.equals("WAITING") ? "text-yellow-600" : status.equals("CHECKED_IN") ? "text-green-600" : "text-[#2563eb]");
            registrationList.add(regMap);
        }

        dto.setTodayTotal(dayAppointments.size());
        dto.setCheckedIn(checkedInCount);
        dto.setWaiting(waitingCount);
        dto.setNewInquiries(waitingCount > 0 ? (int)(totalWaitMinutes / waitingCount) : 0);
        dto.setUpcomingAppointments(upcomingList);
        dto.setCurrentRegistrations(registrationList);
        return dto;
    }
}