package com.example.SmartHospital.dtos.ChatDtos;

import java.time.LocalDateTime;
import java.util.List;


import lombok.Data;

@Data
public class ChatMessageResponse {
    private String id;
    private String doctorId;
    private String doctorName;
    private String patientId;
    private String patientName;
    private String messageText;
    private String senderId;
    private String senderName;
    private LocalDateTime timestamp;
    private List<String> attachmentUrls;
}
