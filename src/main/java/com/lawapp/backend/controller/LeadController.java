package com.lawapp.backend.controller;

import com.lawapp.backend.model.Lead;
import com.lawapp.backend.model.User;
import com.lawapp.backend.service.LeadService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    // Karar-2: Müvekkil telefonu ve kişisel bilgileri maskelenir.
    // Avukat telefon numarasını sadece teklifi kabul edilince görebilir.
    private LeadResponseDto toDto(Lead lead) {
        LeadResponseDto dto = new LeadResponseDto();
        dto.setId(lead.getId());
        dto.setTitle(lead.getTitle());
        dto.setDescription(lead.getDescription());
        dto.setCategory(lead.getCategory());
        dto.setCity(lead.getCity());
        dto.setStatus(lead.getStatus().name());
        dto.setCreatedAt(lead.getCreatedAt() != null ? lead.getCreatedAt().toString() : null);
        // Müvekkil bilgisi — telefon kasıtlı olarak gizlendi
        if (lead.getClient() != null) {
            dto.setClientName(lead.getClient().getFullName());
            // dto.setClientPhone() → KESİNLİKLE KALDIRILDI
        }
        return dto;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createLead(@RequestBody Lead lead) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Lead savedLead = leadService.createLead(email, lead);
            return ResponseEntity.ok(toDto(savedLead));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public List<LeadResponseDto> getAllLeads() {
        return leadService.getAllLeads().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/my-leads")
    public List<LeadResponseDto> getMyLeads() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return leadService.getMyLeads(email).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/matching-lawyers")
    public ResponseEntity<?> getMatchingLawyers(@PathVariable Long id) {
        try {
            List<User> lawyers = leadService.getMatchingLawyers(id);
            
            List<LawyerDto> dtos = lawyers.stream().map(lawyer -> {
                LawyerDto dto = new LawyerDto();
                dto.setId(lawyer.getId());
                dto.setFullName(lawyer.getFullName());
                dto.setAverageRating(lawyer.getAverageRating() != null ? lawyer.getAverageRating() : 5.0);
                dto.setSpecialties(lawyer.getSpecialties());
                dto.setPhoneNumber(lawyer.getPhoneNumber());
                dto.setBarNumber(lawyer.getBarNumber());
                dto.setVerified(lawyer.isVerified());
                if (lawyer.getLawyerProfile() != null) {
                    dto.setBio(lawyer.getLawyerProfile().getBio());
                    dto.setProfileImageUrl(lawyer.getLawyerProfile().getProfileImageUrl());
                    dto.setIntroVideoUrl(lawyer.getLawyerProfile().getIntroVideoUrl());
                }
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class LawyerDto {
        private Long id;
        private String fullName;
        private Double averageRating;
        private java.util.Set<String> specialties;
        private String phoneNumber;
        private String barNumber;
        private boolean verified;
        private String bio;
        private String profileImageUrl;
        private String introVideoUrl;
    }

    @Data
    public static class LeadResponseDto {
        private Long id;
        private String title;
        private String description;
        private String category;
        private String city;
        private String status;
        private String createdAt;
        private String clientName; // İsim görünür ama telefon/email gizli
    }
}

