package com.lawapp.backend.repository;

import com.lawapp.backend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    long countBySessionIdAndSenderIdNotAndReadFalse(Long sessionId, Long senderId);

    List<ChatMessage> findBySessionIdAndSenderIdNotAndReadFalse(Long sessionId, Long senderId);

    // Her oturumun son mesajını bulmak için yardımcı (liste ekranı için)
    ChatMessage findFirstBySessionIdOrderByCreatedAtDesc(Long sessionId);
}
