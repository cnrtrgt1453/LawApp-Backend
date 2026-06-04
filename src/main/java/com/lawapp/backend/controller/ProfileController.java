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
        // Gerçek dünyada burada S3 veya yerel klasöre kayıt yapılır.
        // Şimdilik simüle ediyoruz:
        String imageUrl = "https://lawapp-storage.com/images/" + user.getId() + "_" + file.getOriginalFilename();
        profileService.updateProfileImage(user.getId(), imageUrl);
        return ResponseEntity.ok().body(imageUrl);
    }

    @PostMapping("/upload-video")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        User user = getCurrentUser();
        
        // 90 saniye kontrolü (simüle edilen boyut sınırını 15MB'a indiriyoruz)
        if (file.getSize() > 15 * 1024 * 1024) { // 15MB sınırı
             return ResponseEntity.badRequest().body("Video dosyası çok büyük veya 90 saniyeden uzun (max 15MB).");
        }

        String videoUrl = "https://lawapp-storage.com/videos/" + user.getId() + "_" + file.getOriginalFilename();
        profileService.updateIntroVideo(user.getId(), videoUrl);
        return ResponseEntity.ok().body(videoUrl);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
