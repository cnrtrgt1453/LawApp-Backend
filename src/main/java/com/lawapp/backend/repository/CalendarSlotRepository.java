package com.lawapp.backend.repository;

import com.lawapp.backend.model.CalendarSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarSlotRepository extends JpaRepository<CalendarSlot, Long> {
    List<CalendarSlot> findByLawyerId(Long lawyerId);
    List<CalendarSlot> findByLawyerIdAndAvailableTrue(Long lawyerId);
    Optional<CalendarSlot> findByLawyerIdAndSlotTime(Long lawyerId, LocalDateTime slotTime);
}
