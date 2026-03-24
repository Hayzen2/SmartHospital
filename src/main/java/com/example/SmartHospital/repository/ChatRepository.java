package com.example.SmartHospital.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.model.DoctorPatientMessages;

@Repository
public interface ChatRepository extends JpaRepository<DoctorPatientMessages, String> {
    List<DoctorPatientMessages> findByDoctor_IdAndPatient_IdOrderByTimestampAsc(String doctorId, String patientId);
    List<DoctorPatientMessages> findByDoctorId(String doctorId);
    List<DoctorPatientMessages> findByPatientId(String patientId);
    void markMessagesAsRead(String senderId, String receiverId);
    void markMessagesAsDelivered(String senderId, String receiverId);
    void markMessagesAsSent(String senderId, String receiverId);
}
