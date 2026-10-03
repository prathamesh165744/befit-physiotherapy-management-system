package com.befit.backend.controller;
import com.befit.backend.dto.DashboardDTO;
import com.befit.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    @Autowired private DashboardService dashboardService;

    @GetMapping("/staff-data")
    public ResponseEntity<DashboardDTO> getStaffDashboard(
            @RequestParam(required = false, defaultValue = "KARVE-ROAD") String branchId,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(dashboardService.getReceptionistDashboardData(branchId, date));
    }
}