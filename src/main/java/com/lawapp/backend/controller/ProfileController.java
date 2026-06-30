package com.lawapp.backend.controller;

import com.lawapp.backend.dto.ProfileUpdateDto;
import com.lawapp.backend.model.ClientProfile;
import com.lawapp.backend.model.LawyerProfile;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.UserRepository;
import com.lawapp.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<LawyerProfile> getProfile() {
        User user = getCurrentUser();
        return ResponseEntity.ok(profileService.getProfile(user.getId()));
    }

    @PutMapping
    public ResponseEntity<LawyerProfile> updateProfile(@RequestBody ProfileUpdateDto dto) {
        User user = getCurrentUser();
        return ResponseEntity.ok(profileService.updateProfile(user.getId(), dto));
    }

    // Client Profile Endpoints
    @GetMapping("/client")
    public ResponseEntity<ClientProfile> getMyClientProfile() {
        User user = getCurrentUser();
        return ResponseEntity.ok(profileService.getClientProfile(user.getId()));
    }

    @PutMapping("/client")
    public ResponseEntity<ClientProfile> updateClientProfile(@RequestBody String bio) {
        User user = getCurrentUser();
        return ResponseEntity.ok(profileService.updateClientProfile(user.getId(), bio));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ClientProfile> getClientProfileForLawyer(@PathVariable Long clientId) {
        User lawyer = getCurrentUser();
        return ResponseEntity.ok(profileService.getClientProfileForLawyer(lawyer.getId(), clientId));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        User user = getCurrentUser();
        try {
            String fileName = user.getId() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            java.io.File uploadDir = new java.io.File("uploads/images");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            java.io.File dest = new java.io.File(uploadDir, fileName).getAbsoluteFile();
            file.transferTo(dest);

            String imageUrl = "/uploads/images/" + fileName;
            profileService.updateProfileImage(user.getId(), imageUrl);
            return ResponseEntity.ok().body(imageUrl);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to upload image: " + e.getMessage());
        }
    }


    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
