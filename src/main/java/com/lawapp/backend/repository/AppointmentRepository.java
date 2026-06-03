package com.lawapp.backend.repository;

import com.lawapp.backend.model.Appointment;
import com.lawapp.backend.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByClientId(Long clientId);
    List<Appointment> findByLawyerId(Long lawyerId);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.lawyer.id = :lawyerId AND a.client.id = :clientId AND a.status IN :statuses")
    boolean existsByLawyerIdAndClientIdAndStatusIn(
        @Param("lawyerId") Long lawyerId, 
        @Param("clientId") Long clientId, 
        @Param("statuses") List<AppointmentStatus> statuses
    );

    boolean existsByLawyerIdAndAppointmentTimeAndStatusIn(
        Long lawyerId, 
        LocalDateTime appointmentTime, 
        List<AppointmentStatus> statuses
    );
}
