package com.example.SmartHospital.model;
import java.time.LocalDateTime;

import com.example.SmartHospital.enums.SenderType;
import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "doctor_chatbot_messages")
public class DoctorChatbotMessages {
    @Id
    private String id;

    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateDoctorChatbotMessageId();
        }
        this.timestamp = LocalDateTime.now();
    }

    private String messageText;
    private SenderType sender; 
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;    
}