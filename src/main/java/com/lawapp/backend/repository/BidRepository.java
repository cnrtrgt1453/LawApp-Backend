package com.lawapp.backend.repository;

import com.lawapp.backend.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT b FROM Bid b JOIN FETCH b.lawyer l JOIN FETCH b.lead WHERE b.lead.id = :leadId")
    List<Bid> findByLeadId(@org.springframework.data.repository.query.Param("leadId") Long leadId);
    
    List<Bid> findByLawyerId(Long lawyerId);
    boolean existsByLeadIdAndLawyerId(Long leadId, Long lawyerId);
    boolean existsByLawyerIdAndLeadClientId(Long lawyerId, Long clientId);
}
