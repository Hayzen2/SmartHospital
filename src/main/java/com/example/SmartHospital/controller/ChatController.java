package com.example.SmartHospital.controller;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.ChatDtos.ChatConversationDTO;
import com.example.SmartHospital.dtos.ChatDtos.ChatMessageRequest;
import com.example.SmartHospital.dtos.ChatDtos.ChatMessageResponse;
import com.example.SmartHospital.service.ChatService;
import com.example.SmartHospital.service.MessageStatusService;
import com.example.SmartHospital.service.OnlineStatusService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chat")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final OnlineStatusService onlineStatusService;
    private final MessageStatusService messageStatusService;

    @GetMapping("/getAllChats")
    @Operation(summary = "Get all chat conversations for the authenticated user")
    public ResponseEntity<List<ChatConversationDTO>> getAllChats(@AuthenticationPrincipal String userId, Authentication authentication) {
        try {
            List<ChatConversationDTO> chatHistory = chatService.getAllChats(userId, authentication);
            return ResponseEntity.ok(chatHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }    
    }
    @GetMapping("/getChatHistory")
    @Operation(summary = "Get chat history between the authenticated user and another user")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(@AuthenticationPrincipal String userId, @RequestParam("otherUserId") String otherUserId, Authentication authentication) {
        try {
            List<ChatMessageResponse> chatHistory = chatService.getSpecificChat(userId, otherUserId, authentication);
            return ResponseEntity.ok(chatHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/isOnline")
    @Operation(summary = "Check if a user is online")
    public ResponseEntity<Boolean> getOnlineStatus(@RequestParam("userId") String userId) {
        try {
            boolean isOnline = onlineStatusService.isOnline(userId);
            return ResponseEntity.ok(isOnline);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @MessageMapping("/send")
    @Operation(summary = "Send a message to another user")
    public void sendMessage(ChatMessageRequest message, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to send messages");
        }
        String senderId = principal.getName(); // userId from JWT claims will be used as Principal name
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) principal;
        chatService.createMessage(senderId, message, authentication);
        
    }

    @MessageMapping("/markAsRead")
    @Operation(summary = "Mark messages as read in a conversation")
    public void markAsRead(@RequestParam("otherUserId") String otherUserId, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to mark messages as read");
        }
        String userId = principal.getName(); // userId from JWT claims will be used as Principal name
        messageStatusService.markAsRead(userId, otherUserId);
    }

    @MessageMapping("/markAsDelivered")
    @Operation(summary = "Mark messages as delivered in a conversation")
    public void markAsDelivered(@RequestParam("otherUserId") String otherUserId, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to mark messages as delivered");
        }
        String userId = principal.getName(); // userId from JWT claims will be used as Principal name
        messageStatusService.markAsDelivered(userId, otherUserId);
    }

    @MessageMapping("/markAsSent")
    @Operation(summary = "Mark messages as sent in a conversation")
    public void markAsSent(@RequestParam("otherUserId") String otherUserId, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to mark messages as sent");
        }
        String userId = principal.getName(); // userId from JWT claims will be used as Principal name
        messageStatusService.markAsSent(userId, otherUserId);
    }
}