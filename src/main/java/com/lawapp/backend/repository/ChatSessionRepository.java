package com.lawapp.backend.repository;

import com.lawapp.backend.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByClientIdOrLawyerIdOrderByCreatedAtDesc(Long clientId, Long lawyerId);

    Optional<ChatSession> findByLeadIdAndLawyerId(Long leadId, Long lawyerId);
}
