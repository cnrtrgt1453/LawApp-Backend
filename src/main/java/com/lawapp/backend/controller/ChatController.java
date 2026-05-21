package com.lawapp.backend.controller;

import com.lawapp.backend.model.ChatMessage;
import com.lawapp.backend.model.ChatSession;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.ChatMessageRepository;
import com.lawapp.backend.repository.ChatSessionRepository;
import com.lawapp.backend.repository.UserRepository;
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

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ChatSessionResponseDto>> getMyChats() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<ChatSession> sessions = chatSessionRepository.findByClientIdOrLawyerIdOrderByCreatedAtDesc(
                currentUser.getId(), currentUser.getId());

        List<ChatSessionResponseDto> dtos = sessions.stream().map(session -> {
            ChatSessionResponseDto dto = new ChatSessionResponseDto();
            dto.setId(session.getId());
            dto.setLeadId(session.getLead().getId());
            dto.setLeadTitle(session.getLead().getTitle());

            // Karşı tarafın bilgilerini belirle
            User otherUser = session.getClient().getId().equals(currentUser.getId()) ?
                    session.getLawyer() : session.getClient();

            dto.setOtherParticipantName(otherUser.getFullName());
            dto.setOtherParticipantRole(otherUser.getRole().name());

            // Son mesajı ve okunmamış sayısını çek
            ChatMessage lastMsg = chatMessageRepository.findFirstBySessionIdOrderByCreatedAtDesc(session.getId());
            if (lastMsg != null) {
                dto.setLastMessage(lastMsg.getContent());
                dto.setLastMessageTime(lastMsg.getCreatedAt().toString());
            } else {
                dto.setLastMessage("Henüz mesaj yok. Görüşmeyi başlatın!");
                dto.setLastMessageTime(session.getCreatedAt().toString());
            }

            long unread = chatMessageRepository.countBySessionIdAndSenderIdNotAndReadFalse(
                    session.getId(), currentUser.getId());
            dto.setUnreadCount(unread);

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageResponseDto>> getChatMessages(@PathVariable Long sessionId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // Odaya dahil mi kontrol et
        if (!session.getClient().getId().equals(currentUser.getId()) &&
                !session.getLawyer().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
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
    }

    @PostMapping("/{sessionId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long sessionId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<ChatMessage> unreadMessages = chatMessageRepository.findBySessionIdAndSenderIdNotAndReadFalse(
                sessionId, currentUser.getId());

        unreadMessages.forEach(msg -> msg.setRead(true));
        chatMessageRepository.saveAll(unreadMessages);

        return ResponseEntity.ok().build();
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
