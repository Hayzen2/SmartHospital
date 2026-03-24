package com.example.SmartHospital.dtos.AppointmentDtos.Response.Response;

import com.example.SmartHospital.model.Appointment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppointmentResponse {
    private String appointmentId;
    private String patientName;
    private String doctorName;
    private String appointmentDate;
    private String appointmentTime;
    private String status;
    private String notes;
    private String cancelReason;

    // Constructor to map from Appointment entity (assuming such an entity exists)
    public AppointmentResponse(Appointment appointment) {
        this.appointmentId = String.valueOf(appointment.getId());
        this.patientName = appointment.getPatient().getFullName();
        this.doctorName = appointment.getDoctor().getFullName();
        this.appointmentDate = appointment.getAppointmentDateTime().toLocalDate().toString();
        this.appointmentTime = appointment.getAppointmentDateTime().toLocalTime().toString();
        this.status = appointment.getStatus().name();
        this.notes = appointment.getNotes();
        this.cancelReason = appointment.getCancelReason();
    }
}
