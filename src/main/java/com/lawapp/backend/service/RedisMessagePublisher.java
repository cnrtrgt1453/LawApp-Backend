package com.lawapp.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawapp.backend.config.RedisConfig;
import com.lawapp.backend.dto.RedisChatEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessagePublisher.class);
    
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publishChatEvent(RedisChatEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(RedisConfig.CHAT_TOPIC, message);
            logger.debug("Published chat event to Redis for user: {}", event.getRecipientEmail());
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize RedisChatEvent", e);
        }
    }
}
