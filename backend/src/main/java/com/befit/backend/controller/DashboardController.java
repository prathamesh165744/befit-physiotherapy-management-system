package com.befit.backend.controller;

import com.befit.backend.dto.DashboardDTO;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Autowired private DashboardService dashboardService;
    @Autowired private BranchAccess access;

    @GetMapping("/staff-data")
    public ResponseEntity<DashboardDTO> getStaffDashboard(
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(dashboardService.getReceptionistDashboardData(access.current(), branchId, date));
    }
}
