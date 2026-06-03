package com.lawapp.backend.controller;

import com.lawapp.backend.model.CalendarSlot;
import com.lawapp.backend.service.CalendarService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/lawyer/{lawyerId}")
    public ResponseEntity<List<CalendarSlotDto>> getSlots(@PathVariable Long lawyerId) {
        List<CalendarSlotDto> dtos = calendarService.getSlotsForLawyer(lawyerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/lawyer/{lawyerId}/available")
    public ResponseEntity<List<CalendarSlotDto>> getAvailableSlots(@PathVariable Long lawyerId) {
        List<CalendarSlotDto> dtos = calendarService.getAvailableSlotsForLawyer(lawyerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addSlot(@RequestBody CalendarSlotRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            LocalDateTime time = LocalDateTime.parse(request.getSlotTime());
            CalendarSlot slot = calendarService.addSlot(email, time);
            return ResponseEntity.ok(toDto(slot));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{slotId}")
    public ResponseEntity<?> deleteSlot(@PathVariable Long slotId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            calendarService.deleteSlot(email, slotId);
            return ResponseEntity.ok("Slot silindi");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private CalendarSlotDto toDto(CalendarSlot slot) {
        CalendarSlotDto dto = new CalendarSlotDto();
        dto.setId(slot.getId());
        dto.setLawyerId(slot.getLawyer().getId());
        dto.setSlotTime(slot.getSlotTime().toString());
        dto.setAvailable(slot.isAvailable());
        return dto;
    }

    @Data
    public static class CalendarSlotDto {
        private Long id;
        private Long lawyerId;
        private String slotTime;
        private boolean available;
    }

    @Data
    public static class CalendarSlotRequest {
        private String slotTime; // ISO format (e.g. 2026-06-05T14:00:00)
    }
}
