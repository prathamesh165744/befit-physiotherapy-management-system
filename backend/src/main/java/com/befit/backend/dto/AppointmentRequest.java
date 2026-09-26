package com.befit.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentRequest {
    private Long patientId;
    private Long doctorId;
    private String doctorName;
    private LocalDateTime appointmentDate;
    private String caseType;
}