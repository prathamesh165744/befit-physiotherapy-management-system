package com.befit.backend.service;
import com.befit.backend.dto.DashboardDTO;
import com.befit.backend.entity.Appointment;
import com.befit.backend.entity.AppointmentStatus;
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
    @Autowired
    private AppointmentRepository appointmentRepository;

    public DashboardDTO getReceptionistDashboardData(String branchId) {
        DashboardDTO dto = new DashboardDTO();
        String targetBranch = (branchId != null && !branchId.isEmpty()) ? branchId : "KARVE-ROAD";
        
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        List<Appointment> todayAppointments = appointmentRepository.findByBranchIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(targetBranch, startOfDay, endOfDay);
        List<Appointment> recentCheckins = appointmentRepository.findTop5ByBranchIdAndStatusInOrderByCheckInTimeDesc(targetBranch, List.of(AppointmentStatus.CHECKED_IN, AppointmentStatus.COMPLETED));

        int checkedInCount = 0; int waitingCount = 0; long totalWaitMinutes = 0;
        List<Map<String, Object>> upcomingList = new ArrayList<>();
        List<Map<String, Object>> registrationList = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (Appointment apt : todayAppointments) {
            if (apt.getPatient() == null) continue;
            String status = apt.getStatus() != null ? apt.getStatus().name() : "WAITING";
            
            if (status.equals("CHECKED_IN")) checkedInCount++;
            if (status.equals("WAITING")) {
                waitingCount++;
                if (apt.getCheckInTime() != null) {
                    totalWaitMinutes += Math.max(0, Duration.between(apt.getCheckInTime(), LocalDateTime.now()).toMinutes());
                }
            }

            Map<String, Object> upcomingMap = new HashMap<>();
            upcomingMap.put("time", apt.getAppointmentDate() != null ? apt.getAppointmentDate().format(timeFormatter) : "10:00 AM");
            upcomingMap.put("name", apt.getPatient().getFullName());
            upcomingMap.put("doc", apt.getDoctorName() != null ? apt.getDoctorName() : "Doctor");
            upcomingMap.put("type", apt.getCaseType() != null ? apt.getCaseType() : "Consultation");
            upcomingMap.put("status", status.replace("_", " "));
            upcomingMap.put("color", (status.equals("WAITING") || status.equals("DELAYED")) ? "text-red-600 bg-red-50" : "text-[#2563eb] bg-[#eef2fc]");
            upcomingList.add(upcomingMap);

            Map<String, Object> regMap = new HashMap<>();
            regMap.put("id", "REG-" + apt.getPatient().getId());
            regMap.put("ini", apt.getPatient().getFullName().length() >= 2 ? apt.getPatient().getFullName().substring(0, 2).toUpperCase() : "PT");
            regMap.put("name", apt.getPatient().getFullName());
            regMap.put("age", apt.getPatient().getGender() != null ? "N/A / " + apt.getPatient().getGender() : "N/A / M");
            regMap.put("doc", apt.getDoctorName() != null ? apt.getDoctorName() : "Doctor");
            regMap.put("case", apt.getCaseType() != null ? apt.getCaseType() : "Consultation");
            regMap.put("status", status.replace("_", " "));
            regMap.put("color", status.equals("WAITING") ? "text-yellow-600" : status.equals("CHECKED_IN") ? "text-green-600" : "text-[#2563eb]");
            registrationList.add(regMap);
        }

        dto.setTodayTotal(todayAppointments.size());
        dto.setCheckedIn(checkedInCount);
        dto.setWaiting(waitingCount);
        dto.setNewInquiries(waitingCount > 0 ? (int)(totalWaitMinutes / waitingCount) : 0);
        dto.setUpcomingAppointments(upcomingList);
        dto.setCurrentRegistrations(registrationList);
        return dto;
    }
}