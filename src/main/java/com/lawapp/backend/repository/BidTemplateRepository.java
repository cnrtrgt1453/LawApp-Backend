package com.lawapp.backend.repository;

import com.lawapp.backend.model.BidTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidTemplateRepository extends JpaRepository<BidTemplate, Long> {
    List<BidTemplate> findByLawyerId(Long lawyerId);
}
