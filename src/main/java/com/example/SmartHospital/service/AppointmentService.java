package com.example.SmartHospital.service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.AppointmentDtos.Request.AcceptAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.AppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.CancelAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Response.Response.AppointmentResponse;
import com.example.SmartHospital.dtos.UserDtos.DoctorDTO;
import com.example.SmartHospital.enums.AppointmentStatus;
import com.example.SmartHospital.model.Appointment;
import com.example.SmartHospital.model.Doctor;
import com.example.SmartHospital.repository.AppointmentRepository;
import com.example.SmartHospital.repository.DoctorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    public Page<AppointmentResponse> getPatientAppointments(
            String patientId,
            String search,
            Pageable pageable
    ) {
        return appointmentRepository
                .searchAppointmentsForPatient(patientId, search == null ? "" : search, pageable)
                .map(AppointmentResponse::new);
    }

    public Page<AppointmentResponse> getDoctorAppointments(
            String doctorId,
            String search,
            Pageable pageable
    ) {
        return appointmentRepository
                .searchAppointmentsForDoctor(doctorId, search == null ? "" : search, pageable)
                .map(AppointmentResponse::new);
    }

    public AppointmentResponse createAppointment(AppointmentRequest request) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentDateTime(
            LocalDateTime.of(
                request.getAppointmentDate(),
                request.getAppointmentTime()
            )
        );
        appointment.setNotes(request.getNotes());
        appointment.setStatus(AppointmentStatus.PENDING);
        return new AppointmentResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse cancelAppointment(CancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new RuntimeException("Only SCHEDULED appointments can be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(request.getCancelReason());

        return new AppointmentResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse acceptAppointment(AcceptAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new RuntimeException("Only PENDING appointments can be accepted");
        }

        appointment.setStatus(AppointmentStatus.SCHEDULED);

        return new AppointmentResponse(appointmentRepository.save(appointment));
    }

    public List<DoctorDTO> findAvailableDoctors(
            LocalDate date,
            LocalTime time
    ) {
        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end   = start.plusMinutes(30);

        List<String> busyDoctorIds =
            appointmentRepository.findBusyDoctorIds(
                start,
                end,
                AppointmentStatus.SCHEDULED
            );

        List<Doctor> availableDoctors =
                appointmentRepository.findAvailableDoctors(
                        busyDoctorIds.isEmpty() ? null : busyDoctorIds
                );

        return availableDoctors.stream()
                                .filter(d -> isWithinWorkingHours(d.getWorkingHours(), start, end)) // Filter by working hours
                                .map(d -> {
                                    DoctorDTO dto = new DoctorDTO();
                                    dto.setId(d.getId());
                                    dto.setFullName(d.getFullName());
                                    dto.setEmail(d.getEmail());
                                    dto.setSpecialization(d.getSpecialization());
                                    dto.setWorkingHours(d.getWorkingHours());
                                    dto.setAvailabilityStatus(d.getAvailabilityStatus());
                                    return dto;
                                })
                                .toList();
    }

    // Utility method to check if a time is within working hours
    // For example, "08:00-12:00,14:00-18:00"
    private boolean isWithinWorkingHours(String workingHours, LocalDateTime startTime, LocalDateTime endTime) {
        if (workingHours == null || workingHours.isBlank()) return false;

        String[] ranges = workingHours.split(","); // Split by comma
        for (String range : ranges) {
            String[] parts = range.trim().split("-");
            if (parts.length != 2) { // Invalid range
                continue; 
            }

            LocalDateTime start = LocalDateTime.parse(parts[0].trim());
            LocalDateTime end = LocalDateTime.parse(parts[1].trim());

            if (startTime.isAfter(start) && endTime.isBefore(end)) {
                return true;
            }
        }

        return false;
    }

    public List<LocalTime> getAvailableTimeslots(String doctorId, LocalDate date) {

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        LocalDateTime startDay = date.atStartOfDay();
        LocalDateTime endDay = date.plusDays(1).atStartOfDay();

        // Get booked times given doctor and date
        List<LocalTime> bookedTimes =
                appointmentRepository.findBookedDateTimes(
                    doctorId,
                    startDay,
                    endDay,
                    AppointmentStatus.SCHEDULED
                );

        // Get available times
        List<LocalTime> available = new ArrayList<>();

        // If no working hours, return all times
        if (doctor.getWorkingHours() == null || doctor.getWorkingHours().isBlank()) {
            return available;
        }

        // Example: "09:00-12:00,13:00-17:00"
        String[] ranges = doctor.getWorkingHours().split(",");

        for (String range : ranges) {
            String[] parts = range.trim().split("-");

            if (parts.length != 2) { // Invalid range
                continue;
            }
            LocalTime startSlot = LocalTime.parse(parts[0].trim());
            LocalTime endSlot = LocalTime.parse(parts[1].trim());

            LocalTime slot = startSlot;

            while (slot.isBefore(endSlot)) {
                if (!bookedTimes.contains(slot)) {
                    available.add(slot);
                }
                slot = slot.plusMinutes(30); 
            }
        }

        return available;
    }

}