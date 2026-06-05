package com.lawapp.backend.service;

import com.lawapp.backend.dto.ChatSessionSummaryProjection;
import com.lawapp.backend.model.ChatMessage;
import com.lawapp.backend.model.ChatSession;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.ChatMessageRepository;
import com.lawapp.backend.repository.ChatSessionRepository;
import com.lawapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public List<ChatSession> getMyChatSessions(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return chatSessionRepository.findByClientIdOrLawyerIdOrderByCreatedAtDesc(
                currentUser.getId(), currentUser.getId());
    }

    public List<ChatSessionSummaryProjection> getMyChatSessionSummaries(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return chatSessionRepository.findChatSessionSummariesByUserId(currentUser.getId());
    }

    public List<ChatMessage> getChatMessages(String email, Long sessionId) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // Odaya dahil mi kontrol et
        if (!session.getClient().getId().equals(currentUser.getId()) &&
                !session.getLawyer().getId().equals(currentUser.getId())) {
            throw new SecurityException("Unauthorized access to chat session");
        }

        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public void markMessagesAsRead(String email, Long sessionId) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<ChatMessage> unreadMessages = chatMessageRepository.findBySessionIdAndSenderIdNotAndReadFalse(
                sessionId, currentUser.getId());

        unreadMessages.forEach(msg -> msg.setRead(true));
        chatMessageRepository.saveAll(unreadMessages);
    }
}
