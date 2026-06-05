package com.lawapp.backend.controller;

import com.lawapp.backend.dto.ChatSessionSummaryProjection;
import com.lawapp.backend.model.ChatMessage;
import com.lawapp.backend.model.ChatSession;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.ChatMessageRepository;
import com.lawapp.backend.repository.UserRepository;
import com.lawapp.backend.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;

    @GetMapping
    public ResponseEntity<List<ChatSessionResponseDto>> getMyChats() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            List<ChatSessionSummaryProjection> summaries = chatService.getMyChatSessionSummaries(email);

            List<ChatSessionResponseDto> dtos = summaries.stream().map(summary -> {
                ChatSessionResponseDto dto = new ChatSessionResponseDto();
                dto.setId(summary.getId());
                dto.setLeadId(summary.getLeadId());
                dto.setLeadTitle(summary.getLeadTitle());
                dto.setOtherParticipantName(summary.getOtherParticipantName());
                dto.setOtherParticipantRole(summary.getOtherParticipantRole());

                if (summary.getLastMessage() != null) {
                    dto.setLastMessage(summary.getLastMessage());
                    dto.setLastMessageTime(summary.getLastMessageTime().toString());
                } else {
                    dto.setLastMessage("Henüz mesaj yok. Görüşmeyi başlatın!");
                    // Fallback to session creation time would ideally come from summary as well,
                    // but for simplicity we return an empty string or null if not available in projection.
                    dto.setLastMessageTime(""); 
                }

                dto.setUnreadCount(summary.getUnreadCount());
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageResponseDto>> getChatMessages(@PathVariable Long sessionId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            List<ChatMessage> messages = chatService.getChatMessages(email, sessionId);
            List<ChatMessageResponseDto> dtos = messages.stream().map(msg -> {
                ChatMessageResponseDto dto = new ChatMessageResponseDto();
                dto.setId(msg.getId());
                dto.setSenderEmail(msg.getSender().getEmail());
                dto.setSenderName(msg.getSender().getFullName());
                dto.setContent(msg.getContent());
                dto.setCreatedAt(msg.getCreatedAt().toString());
                dto.setRead(msg.isRead());
                dto.setFileUrl(msg.getFileUrl());
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{sessionId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long sessionId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            chatService.markMessagesAsRead(email, sessionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Data
    public static class ChatSessionResponseDto {
        private Long id;
        private Long leadId;
        private String leadTitle;
        private String otherParticipantName;
        private String otherParticipantRole;
        private String lastMessage;
        private String lastMessageTime;
        private long unreadCount;
    }

    @Data
    public static class ChatMessageResponseDto {
        private Long id;
        private String senderEmail;
        private String senderName;
        private String content;
        private String createdAt;
        private boolean read;
        private String fileUrl;
    }
}
