package com.lawapp.backend.dto;

import java.time.LocalDateTime;

public interface ChatSessionSummaryProjection {
    Long getId();
    Long getLeadId();
    String getLeadTitle();
    String getOtherParticipantName();
    String getOtherParticipantRole();
    String getLastMessage();
    LocalDateTime getLastMessageTime();
    Long getUnreadCount();
}
