package com.lawapp.backend.repository;

import com.lawapp.backend.dto.ChatSessionSummaryProjection;
import com.lawapp.backend.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByClientIdOrLawyerIdOrderByCreatedAtDesc(Long clientId, Long lawyerId);

    Optional<ChatSession> findByLeadIdAndLawyerId(Long leadId, Long lawyerId);

    @Query(value = """
        SELECT 
            cs.id as id,
            l.id as leadId,
            l.title as leadTitle,
            CASE WHEN cs.client_id = :userId THEN u_lawyer.full_name ELSE u_client.full_name END as otherParticipantName,
            CASE WHEN cs.client_id = :userId THEN 'LAWYER' ELSE 'CLIENT' END as otherParticipantRole,
            (SELECT content FROM chat_messages m WHERE m.session_id = cs.id ORDER BY m.created_at DESC LIMIT 1) as lastMessage,
            (SELECT created_at FROM chat_messages m WHERE m.session_id = cs.id ORDER BY m.created_at DESC LIMIT 1) as lastMessageTime,
            (SELECT COUNT(*) FROM chat_messages cm WHERE cm.session_id = cs.id AND cm.sender_id != :userId AND cm.is_read = false) as unreadCount
        FROM chat_sessions cs
        JOIN leads l ON cs.lead_id = l.id
        JOIN users u_client ON cs.client_id = u_client.id
        JOIN users u_lawyer ON cs.lawyer_id = u_lawyer.id
        WHERE cs.client_id = :userId OR cs.lawyer_id = :userId
        ORDER BY COALESCE((SELECT created_at FROM chat_messages m WHERE m.session_id = cs.id ORDER BY m.created_at DESC LIMIT 1), cs.created_at) DESC
    """, nativeQuery = true)
    List<ChatSessionSummaryProjection> findChatSessionSummariesByUserId(@Param("userId") Long userId);
}
