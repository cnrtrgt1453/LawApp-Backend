package com.lawapp.backend.controller;

import com.lawapp.backend.model.Appointment;
import com.lawapp.backend.model.Role;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.UserRepository;
import com.lawapp.backend.service.AppointmentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    @PostMapping("/book")
    public ResponseEntity<?> book(@RequestBody BookAppointmentRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            LocalDateTime time = LocalDateTime.parse(request.getAppointmentTime());
            Appointment appointment = appointmentService.createAppointment(
                    email, request.getLawyerId(), request.getLeadId(), time
            );
            return ResponseEntity.ok(toDto(appointment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Appointment appointment = appointmentService.acceptAppointment(email, id);
            return ResponseEntity.ok(toDto(appointment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Appointment appointment = appointmentService.rejectAppointment(email, id);
            return ResponseEntity.ok(toDto(appointment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyAppointments() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow();

        List<Appointment> list;
        if (currentUser.getRole() == Role.CLIENT) {
            list = appointmentService.getAppointmentsForClient(email);
        } else {
            list = appointmentService.getAppointmentsForLawyer(email);
        }

        List<AppointmentDto> dtos = list.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    private AppointmentDto toDto(Appointment appointment) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(appointment.getId());
        dto.setClientId(appointment.getClient().getId());
        dto.setClientName(appointment.getClient().getFullName());
        dto.setLawyerId(appointment.getLawyer().getId());
        dto.setLawyerName(appointment.getLawyer().getFullName());
        dto.setAppointmentTime(appointment.getAppointmentTime().toString());
        dto.setStatus(appointment.getStatus().name());
        dto.setPlatformFee(appointment.getPlatformFee());
        dto.setPaymentStatus(appointment.getPaymentStatus());
        dto.setRoomId(appointment.getRoomId());
        if (appointment.getLead() != null) {
            dto.setLeadId(appointment.getLead().getId());
            dto.setLeadTitle(appointment.getLead().getTitle());
            dto.setLeadCategory(appointment.getLead().getCategory());
        } else {
            dto.setLeadTitle("Genel Danışmanlık");
            dto.setLeadCategory("Genel");
        }
        return dto;
    }

    @Data
    public static class BookAppointmentRequest {
        private Long lawyerId;
        private Long leadId;
        private String appointmentTime;
    }

    @Data
    public static class AppointmentDto {
        private Long id;
        private Long clientId;
        private String clientName;
        private Long lawyerId;
        private String lawyerName;
        private String appointmentTime;
        private String status;
        private BigDecimal platformFee;
        private String paymentStatus;
        private String roomId;
        private Long leadId;
        private String leadTitle;
        private String leadCategory;
    }
}
