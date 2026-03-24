package com.example.SmartHospital.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.SmartHospital.enums.MessageStatus;
import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "doctor_patient_messages")
public class DoctorPatientMessages {
    @Id
    private String id;
    
    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateDoctorPatientMessageId();
        }
        this.timestamp = LocalDateTime.now();
    }

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(nullable = false)
    private String senderId; // ID of the sender (doctor or patient)

    @Column(nullable = false)
    private String messageText;

    private LocalDateTime timestamp;

    private MessageStatus status;

    @ElementCollection
    private List<String> attachments;

}
