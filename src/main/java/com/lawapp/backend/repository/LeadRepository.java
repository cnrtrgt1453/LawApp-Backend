package com.lawapp.backend.repository;

import com.lawapp.backend.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    List<Lead> findByCategory(String category);
    List<Lead> findByClientId(Long clientId);
}
