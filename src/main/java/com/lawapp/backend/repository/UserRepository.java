package com.lawapp.backend.repository;

import com.lawapp.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u JOIN u.specialties s WHERE u.role = com.lawapp.backend.model.Role.LAWYER AND LOWER(s) = LOWER(:specialty)")
    List<User> findLawyersBySpecialty(@Param("specialty") String specialty);
}
