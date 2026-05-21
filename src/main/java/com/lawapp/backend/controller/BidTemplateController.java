package com.lawapp.backend.controller;

import com.lawapp.backend.model.BidTemplate;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.BidTemplateRepository;
import com.lawapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class BidTemplateController {

    private final BidTemplateRepository bidTemplateRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<BidTemplate>> getMyTemplates() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User lawyer = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(bidTemplateRepository.findByLawyerId(lawyer.getId()));
    }

    @PostMapping
    public ResponseEntity<BidTemplate> createTemplate(@RequestBody BidTemplate template) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User lawyer = userRepository.findByEmail(email).orElseThrow();
        
        template.setLawyer(lawyer);
        return ResponseEntity.ok(bidTemplateRepository.save(template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User lawyer = userRepository.findByEmail(email).orElseThrow();
        
        BidTemplate template = bidTemplateRepository.findById(id).orElseThrow();
        if (!template.getLawyer().getId().equals(lawyer.getId())) {
            return ResponseEntity.badRequest().body("Not your template");
        }
        
        bidTemplateRepository.delete(template);
        return ResponseEntity.ok().build();
    }
}
