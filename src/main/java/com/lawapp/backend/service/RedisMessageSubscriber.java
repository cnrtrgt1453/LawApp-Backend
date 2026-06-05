package com.lawapp.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawapp.backend.dto.RedisChatEvent;
import com.lawapp.backend.security.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private final ObjectMapper objectMapper;
    // Lazy to avoid circular dependency
    @Lazy
    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            RedisChatEvent event = objectMapper.readValue(body, RedisChatEvent.class);
            
            // Eğer bu node'da bağlı ise gönder
            chatWebSocketHandler.sendMessageToUser(event.getRecipientEmail(), event.getMessageJson());
        } catch (Exception e) {
            logger.error("Error processing Redis message", e);
        }
    }
}
