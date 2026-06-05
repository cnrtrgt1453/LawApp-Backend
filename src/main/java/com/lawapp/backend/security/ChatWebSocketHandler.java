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
        // JWT doğrulaması artık JwtHandshakeInterceptor tarafından yapılıyor.
        // Burada sadece interceptor'ın set ettiği email attribute'unu kontrol ediyoruz.
        String email = (String) session.getAttributes().get("email");
        Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");

        if (email != null && Boolean.TRUE.equals(authenticated)) {
            activeSessions.put(email, session);
            logger.info("WebSocket connection established for user: {}", maskEmail(email));
        } else {
            logger.warn("WebSocket connection rejected: Missing authentication attributes from handshake");
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
                logger.warn("User {} tried to send message to unauthorized chat session {}", maskEmail(email), payload.getSessionId());
                return;
            }

            // Mesaj içeriğini sanitize et (XSS koruması)
            String sanitizedContent = sanitizeInput(payload.getContent());
            String sanitizedFileUrl = sanitizeFileUrl(payload.getFileUrl());

            // Mesajı kaydet
            ChatMessage chatMessage = ChatMessage.builder()
                    .session(chatSession)
                    .sender(sender)
                    .content(sanitizedContent)
                    .fileUrl(sanitizedFileUrl)
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
                logger.info("Message sent in real-time to online user: {}", maskEmail(recipient.getEmail()));
            } else {
                // Çevrimdışı ise push notification gönder
                notificationService.sendNotification(
                        recipient.getId(),
                        "Yeni Mesaj: " + sender.getFullName(),
                        sanitizedContent
                );
                logger.info("User {} is offline. Push notification triggered.", maskEmail(recipient.getEmail()));
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
            logger.info("WebSocket connection closed for user: {}", maskEmail(email));
        }
    }

    // ===================== Güvenlik Yardımcı Metotları =====================

    /**
     * HTML/JS etiketlerini ve tehlikeli karakterleri HTML entity'lerine dönüştürür.
     * XSS (Cross-Site Scripting) saldırılarını engeller.
     */
    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * Dosya URL'sini doğrular. Sadece http/https URL'lerine izin verir.
     * Kötü niyetli javascript: veya data: URI şemalarını engeller.
     */
    private String sanitizeFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return null;
        String trimmed = fileUrl.trim();
        if (trimmed.toLowerCase().startsWith("http://") || trimmed.toLowerCase().startsWith("https://")) {
            return sanitizeInput(trimmed);
        }
        logger.warn("Rejected invalid file URL scheme: {}", trimmed.substring(0, Math.min(trimmed.length(), 20)));
        return null;
    }

    /**
     * E-posta adresini log'larda maskeleyerek gösterir.
     * Örnek: "caner@example.com" → "ca***@example.com"
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() <= 2
                ? local + "***"
                : local.substring(0, 2) + "***";
        return masked + "@" + parts[1];
    }

    // ===================== Payload Sınıfları =====================

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
