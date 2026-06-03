package com.lawapp.backend.service;

import com.lawapp.backend.model.CalendarSlot;
import com.lawapp.backend.model.Role;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.CalendarSlotRepository;
import com.lawapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarSlotRepository calendarSlotRepository;
    private final UserRepository userRepository;

    public List<CalendarSlot> getSlotsForLawyer(Long lawyerId) {
        return calendarSlotRepository.findByLawyerId(lawyerId);
    }

    public List<CalendarSlot> getAvailableSlotsForLawyer(Long lawyerId) {
        return calendarSlotRepository.findByLawyerIdAndAvailableTrue(lawyerId);
    }

    @Transactional
    public CalendarSlot addSlot(String lawyerEmail, LocalDateTime slotTime) {
        User lawyer = userRepository.findByEmail(lawyerEmail)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));

        if (lawyer.getRole() != Role.LAWYER) {
            throw new RuntimeException("Only lawyers can add calendar slots");
        }

        if (slotTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot add calendar slots in the past");
        }

        calendarSlotRepository.findByLawyerIdAndSlotTime(lawyer.getId(), slotTime)
                .ifPresent(s -> {
                    throw new RuntimeException("Slot at this time already exists");
                });

        CalendarSlot slot = CalendarSlot.builder()
                .lawyer(lawyer)
                .slotTime(slotTime)
                .available(true)
                .build();

        return calendarSlotRepository.save(slot);
    }

    @Transactional
    public void deleteSlot(String lawyerEmail, Long slotId) {
        User lawyer = userRepository.findByEmail(lawyerEmail)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));

        CalendarSlot slot = calendarSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.getLawyer().getId().equals(lawyer.getId())) {
            throw new RuntimeException("You can only delete your own slots");
        }

        if (!slot.isAvailable()) {
            throw new RuntimeException("Cannot delete a booked calendar slot");
        }

        calendarSlotRepository.delete(slot);
    }
}
