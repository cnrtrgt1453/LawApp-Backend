package com.lawapp.backend.repository;

import com.lawapp.backend.model.LawyerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LawyerProfileRepository extends JpaRepository<LawyerProfile, Long> {
}
