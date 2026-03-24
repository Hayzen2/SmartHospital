package com.example.SmartHospital.dtos.AppointmentDtos.Request;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AppointmentRequest {
    private String doctorId;
    @Schema(description = "Date of the appointment", example = "2023-05-01")
    private LocalDate appointmentDate;
    @Schema(description = "Time of the appointment", example = "10:00", format="time")
    private LocalTime appointmentTime;
    private String notes;
}
