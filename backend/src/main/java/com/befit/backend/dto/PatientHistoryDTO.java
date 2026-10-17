package com.befit.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** A patient's visits, invoices and branch moves. Every row keeps the branch where it really happened. */
public record PatientHistoryDTO(List<AppointmentDTO> appointments, List<BillDTO> bills, List<BranchStint> branchHistory) {
    public record BranchStint(String branchId, String branchName, LocalDateTime fromDate, LocalDateTime toDate,
                              String authorizedBy, String reason) {}
}
