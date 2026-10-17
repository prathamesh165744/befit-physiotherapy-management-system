package com.befit.backend.controller;

import com.befit.backend.dto.AppointmentCreateRequest;
import com.befit.backend.dto.AppointmentDTO;
import com.befit.backend.dto.PageResponse;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final BranchAccess access;

    public AppointmentController(AppointmentService appointmentService, BranchAccess access) {
        this.appointmentService = appointmentService;
        this.access = access;
    }

    /** Paged. ?date=yyyy-MM-dd &statuses=COMPLETED,CANCELLED &page=0 &size=20 &dir=asc|desc  (branchId only honoured for admins) */
    @GetMapping
    public PageResponse<AppointmentDTO> list(@RequestParam(required = false) String branchId,
                                             @RequestParam(required = false) String date,
                                             @RequestParam(required = false) String statuses,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "desc") String dir) {
        return appointmentService.list(access.current(), branchId, date, statuses, page, size, dir);
    }

    /** Books at the patient's current branch. */
    @PostMapping
    public AppointmentDTO create(@Valid @RequestBody AppointmentCreateRequest request) {
        return appointmentService.create(access.current(), request);
    }

    /** Body: { "status": "CHECKED_IN" } */
    @PutMapping("/{id}/status")
    public AppointmentDTO updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return appointmentService.updateStatus(access.current(), id, payload.get("status"));
    }

    @GetMapping("/patient/{patientId}")
    public List<AppointmentDTO> forPatient(@PathVariable Long patientId) {
        return appointmentService.forPatient(access.current(), patientId);
    }
}
