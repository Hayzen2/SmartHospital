package com.example.SmartHospital.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AppointmentDtos.Request.AcceptAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.AppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.CancelAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Response.Response.AppointmentResponse;
import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.UserDtos.DoctorDTO;
import com.example.SmartHospital.service.AppointmentService;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/appointment")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/patient/getAppointments")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getPatientAppointments(
        @AuthenticationPrincipal String userId,
        @RequestParam(required = false, defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "appointmentDateTime") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.fromString(direction), sortBy)
        );

        Page<AppointmentResponse> result =
                appointmentService.getPatientAppointments(userId, search, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointments retrieved successfully", result)
        );
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/getAppointments")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getDoctorAppointments(
        @AuthenticationPrincipal String userId,
        @RequestParam(required = false, defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "appointmentDateTime") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.fromString(direction), sortBy)
        );

        Page<AppointmentResponse> result =
            appointmentService.getDoctorAppointments(userId, search, pageable);

        return ResponseEntity.ok(
            new ApiResponse<>(200, "Appointments retrieved successfully", result)
        );
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/createAppointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(@RequestBody AppointmentRequest request) {
        try {
            AppointmentResponse response = appointmentService.createAppointment(request);
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "Appointment created successfully", response)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Failed to create appointment: " + e.getMessage(), null));
        }
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/cancelAppointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(@RequestBody CancelAppointmentRequest request) {
        try {
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "Appointment cancelled successfully", appointmentService.cancelAppointment(request))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Failed to cancel appointment: " + e.getMessage(), null));
        }
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/acceptAppointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> acceptAppointment(@RequestBody AcceptAppointmentRequest request) {
        try {
            return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointment accepted successfully", appointmentService.acceptAppointment(request))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Failed to accept appointment: " + e.getMessage(), null));
        }
    }

    @GetMapping("/available-doctors") 
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')") 
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getAvailableDoctors( 
        @Schema(description = "Date of the appointment", example = "2023-05-01", format="date") 
        @RequestParam LocalDate date, 
        @Schema(description = "Time of the appointment", example = "10:00", format="time") 
        @RequestParam LocalTime time ) { 
            List<DoctorDTO> doctors = appointmentService.findAvailableDoctors(date, time); 
            return ResponseEntity.ok(new ApiResponse<>(200, "Available doctors retrieved", doctors)); 
    }

    @GetMapping("/available-timeslots")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<LocalTime>>> getAvailableTimeslots(
        @RequestParam String doctorId,
        @Schema(description = "Date of the appointment", example = "2023-05-01", format="date")
        @RequestParam LocalDate date
    ) {
        try{
            List<LocalTime> timeslots = appointmentService.getAvailableTimeslots(doctorId, date);
            return ResponseEntity.ok(new ApiResponse<>(200, "Available timeslots retrieved", timeslots));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to get available timeslots", null));
        }
    }
}
