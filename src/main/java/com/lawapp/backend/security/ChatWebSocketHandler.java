package com.lawapp.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawapp.backend.model.ChatMessage;
import com.lawapp.backend.model.ChatSession;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.ChatMessageRepository;
import com.lawapp.backend.repository.ChatSessionRepository;
import com.lawapp.backend.repository.UserRepository;
import com.lawapp.backend.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    private final JwtUtils jwtUtils;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        String token = parseToken(query);

        if (token != null && jwtUtils.validateJwtToken(token)) {
            String email = jwtUtils.getUserNameFromJwtToken(token);
            session.getAttributes().put("email", email);
            activeSessions.put(email, session);
            logger.info("WebSocket connection established for user: {}", email);
        } else {
            logger.warn("WebSocket connection rejected: Invalid JWT token");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String email = (String) session.getAttributes().get("email");
        if (email == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        try {
            IncomingMessagePayload payload = objectMapper.readValue(message.getPayload(), IncomingMessagePayload.class);
            ChatSession chatSession = chatSessionRepository.findById(payload.getSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("ChatSession not found"));

            User sender = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Sender user not found"));

            // Gönderici bu sohbet odasına dahil mi kontrol et
            boolean isClient = chatSession.getClient().getId().equals(sender.getId());
            boolean isLawyer = chatSession.getLawyer().getId().equals(sender.getId());

            if (!isClient && !isLawyer) {
                logger.warn("User {} tried to send message to unauthorized chat session {}", email, payload.getSessionId());
                return;
            }

            // Mesajı kaydet
            ChatMessage chatMessage = ChatMessage.builder()
                    .session(chatSession)
                    .sender(sender)
                    .content(payload.getContent())
                    .fileUrl(payload.getFileUrl())
                    .read(false)
                    .build();

            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

            // Alıcıyı belirle
            User recipient = isClient ? chatSession.getLawyer() : chatSession.getClient();

            // Mesajı alıcıya gerçek zamanlı gönder
            WebSocketSession recipientSession = activeSessions.get(recipient.getEmail());
            
            OutgoingMessagePayload responsePayload = new OutgoingMessagePayload();
            responsePayload.setId(savedMessage.getId());
            responsePayload.setSessionId(chatSession.getId());
            responsePayload.setSenderEmail(sender.getEmail());
            responsePayload.setSenderName(sender.getFullName());
            responsePayload.setContent(savedMessage.getContent());
            responsePayload.setCreatedAt(LocalDateTime.now().toString());
            responsePayload.setFileUrl(savedMessage.getFileUrl());

            String jsonResponse = objectMapper.writeValueAsString(responsePayload);
            TextMessage textResponse = new TextMessage(jsonResponse);

            // Kendisine de eko gönder (doğrulama için)
            if (session.isOpen()) {
                session.sendMessage(textResponse);
            }

            if (recipientSession != null && recipientSession.isOpen()) {
                recipientSession.sendMessage(textResponse);
                logger.info("Message sent in real-time to online user: {}", recipient.getEmail());
            } else {
                // Çevrimdışı ise push notification gönder
                notificationService.sendNotification(
                        recipient.getId(),
                        "Yeni Mesaj: " + sender.getFullName(),
                        payload.getContent()
                );
                logger.info("User {} is offline. Push notification triggered.", recipient.getEmail());
            }

        } catch (Exception e) {
            logger.error("Error processing WebSocket text message: ", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String email = (String) session.getAttributes().get("email");
        if (email != null) {
            activeSessions.remove(email);
            logger.info("WebSocket connection closed for user: {}", email);
        }
    }

    private String parseToken(String query) {
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }

    @Data
    public static class IncomingMessagePayload {
        private Long sessionId;
        private String content;
        private String fileUrl;
    }

    @Data
    public static class OutgoingMessagePayload {
        private Long id;
        private Long sessionId;
        private String senderEmail;
        private String senderName;
        private String content;
        private String createdAt;
        private String fileUrl;
    }
}
