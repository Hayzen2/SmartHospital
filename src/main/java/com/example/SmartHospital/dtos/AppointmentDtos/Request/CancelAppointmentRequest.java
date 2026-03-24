package com.example.SmartHospital.dtos.AppointmentDtos.Request;

import lombok.Data;

@Data
public class CancelAppointmentRequest {
    private String appointmentId;
    private String cancelReason;
}
