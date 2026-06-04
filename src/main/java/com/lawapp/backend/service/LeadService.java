package com.lawapp.backend.service;

import com.lawapp.backend.model.Lead;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.LeadRepository;
import com.lawapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Lead createLead(String email, Lead lead) {
        User client = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!client.getRole().name().equals("CLIENT")) {
            throw new IllegalArgumentException("Only clients can create leads.");
        }

        lead.setClient(client);
        Lead savedLead = leadRepository.save(lead);

        // Avukatlara haber ver
        notificationService.notifyLawyersAboutNewLead(savedLead.getCategory(), savedLead.getTitle());

        return savedLead;
    }

    public List<Lead> getAllLeads() {
        return leadRepository.findAll();
    }

    public List<Lead> getMyLeads(String email) {
        User client = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        return leadRepository.findByClientId(client.getId());
    }

    public List<User> getMatchingLawyers(Long leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        return userRepository.findLawyersBySpecialty(lead.getCategory());
    }
}
