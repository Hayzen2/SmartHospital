package com.example.SmartHospital.dtos.ChatDtos;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatConversationDTO {
    private String conversationId;
    private String otherUserId;
    private String otherUserName;
    private boolean onlineStatus;
    private String lastMessage;
    private LocalDateTime lastMessageTimestamp;
    private int unreadMessageCount;
}
