package com.example.SmartHospital.service;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.repository.ChatRepository;
import com.example.SmartHospital.enums.MessageStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageStatusService {
    private final ChatRepository chatRepository;
    private final WebSocketMessagingService websocketMessagingService;
    private final MessageStatus messageStatus;

    // Traansactional is important to ensure that the status update 
    // and the notification happen atomically, preventing race conditions 
    // where the notification is sent before the status is updated in the 
    // database. This ensures data consistency and that the receiver 
    // gets accurate status updates in real-time
    @Transactional
    public void markAsRead(String senderId, String receiverId) {
        chatRepository.markMessagesAsRead(senderId, receiverId);
        websocketMessagingService.sendMessageStatus(senderId, receiverId, MessageStatus.READ);
    }

    @Transactional
    public void markAsDelivered(String senderId, String receiverId) {
        chatRepository.markMessagesAsDelivered(senderId, receiverId);
        websocketMessagingService.sendMessageStatus(senderId, receiverId, MessageStatus.DELIVERED);
    }

    @Transactional
    public void markAsSent(String senderId, String receiverId) {
        chatRepository.markMessagesAsSent(senderId, receiverId);
        websocketMessagingService.sendMessageStatus(senderId, receiverId, MessageStatus.SENT);
    }
}
