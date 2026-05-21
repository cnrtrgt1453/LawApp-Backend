package com.lawapp.backend.repository;

import com.lawapp.backend.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByLeadId(Long leadId);
    List<Bid> findByLawyerId(Long lawyerId);
    boolean existsByLeadIdAndLawyerId(Long leadId, Long lawyerId);
    boolean existsByLawyerIdAndLeadClientId(Long lawyerId, Long clientId);
}
