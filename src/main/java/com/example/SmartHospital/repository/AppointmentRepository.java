package com.example.SmartHospital.repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.enums.AppointmentStatus;
import com.example.SmartHospital.model.Appointment;
import com.example.SmartHospital.model.Doctor;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.patient.id = :patientId
            AND (
                LOWER(a.doctor.fullName) LIKE LOWER(CONCAT(:search, '%')) 
            OR LOWER(a.doctor.email) LIKE LOWER(CONCAT(:search, '%'))
          )
    """)
    Page<Appointment> searchAppointmentsForPatient(
        @Param("patientId") String patientId,
        @Param("search") String search,
        Pageable pageable
    );

    
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.doctor.id = :doctorId
          AND (
                LOWER(a.patient.fullName) LIKE LOWER(CONCAT(:search, '%'))
             OR LOWER(a.patient.email) LIKE LOWER(CONCAT(:search, '%'))
          )
    """)
    Page<Appointment> searchAppointmentsForDoctor(
        @Param("doctorId") String doctorId,
        @Param("search") String search,
        Pageable pageable
    );

    // Find busy doctor IDs
    @Query("""
        SELECT a.doctor.id
        FROM Appointment a
        WHERE a.appointmentDateTime BETWEEN :start AND :end
        AND a.status = :status
    """)
    List<String> findBusyDoctorIds(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") AppointmentStatus status
    );

    // Find available doctors
    @Query("""
        SELECT d FROM Doctor d
        WHERE d.availabilityStatus = 'AVAILABLE'
          AND (:busyIds IS NULL OR d.id NOT IN :busyIds)
    """)
    List<Doctor> findAvailableDoctors(@Param("busyIds") List<String> busyIds);

    // Find booked times
    @Query("""
        SELECT a.appointmentDateTime
        FROM Appointment a
        WHERE a.doctor.id = :doctorId
            AND a.appointmentDateTime BETWEEN :start AND :end
            AND a.status = :status
        """)
    List<LocalTime> findBookedDateTimes(
        @Param("doctorId") String doctorId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("status") AppointmentStatus status
    );
}
