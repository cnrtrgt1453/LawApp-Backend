package com.lawapp.backend.service;

import com.lawapp.backend.dto.ProfileUpdateDto;
import com.lawapp.backend.dto.ClientProfileUpdateDto;
import com.lawapp.backend.model.ClientProfile;
import com.lawapp.backend.model.LawyerProfile;
import com.lawapp.backend.model.Role;
import com.lawapp.backend.model.User;
import com.lawapp.backend.model.AppointmentStatus;
import com.lawapp.backend.repository.AppointmentRepository;
import com.lawapp.backend.repository.ClientProfileRepository;
import com.lawapp.backend.repository.LawyerProfileRepository;
import com.lawapp.backend.repository.UserRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final LawyerProfileRepository profileRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    public LawyerProfile getProfile(Long userId) {
        return profileRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    
                    if (user.getRole() != Role.LAWYER) {
                        throw new RuntimeException("Only lawyers have profiles");
                    }

                    LawyerProfile profile = LawyerProfile.builder()
                            .user(user)
                            .build();
                    return profileRepository.save(profile);
                });
    }

    // Client Profile methods
    public ClientProfile getClientProfile(Long userId) {
        return clientProfileRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    if (user.getRole() != Role.CLIENT) {
                        throw new RuntimeException("Only clients have profiles");
                    }

                    ClientProfile profile = ClientProfile.builder()
                            .user(user)
                            .build();
                    return clientProfileRepository.save(profile);
                });
    }

    public ClientProfile getClientProfileForLawyer(Long lawyerId, Long clientId) {
        boolean hasAppointment = appointmentRepository.existsByLawyerIdAndClientIdAndStatusIn(
                lawyerId, clientId, Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.ACCEPTED, AppointmentStatus.COMPLETED)
        );
        if (!hasAppointment) {
            throw new RuntimeException("Access denied: You must have an active appointment request with this client to view their profile.");
        }
        return getClientProfile(clientId);
    }

    @Transactional
    public ClientProfile updateClientProfile(Long userId, ClientProfileUpdateDto dto) {
        ClientProfile profile = getClientProfile(userId);
        profile.setBio(dto.getBio());
        
        User user = profile.getUser();
        if (user != null && dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            user.setFullName(dto.getFullName().trim());
            userRepository.save(user);
        }
        
        return clientProfileRepository.save(profile);
    }

    @Transactional
    public LawyerProfile updateProfile(Long userId, ProfileUpdateDto dto) {
        LawyerProfile profile = getProfile(userId);
        
        profile.setBio(dto.getBio());
        profile.setLinkedinUrl(dto.getLinkedinUrl());
        profile.setInstagramUrl(dto.getInstagramUrl());
        profile.setWebsiteUrl(dto.getWebsiteUrl());
        profile.setYoutubeUrl(dto.getYoutubeUrl());
        profile.setCity(dto.getCity());
        
        User user = profile.getUser();
        if (user != null) {
            if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
                user.setFullName(dto.getFullName().trim());
            }
            user.getSpecialties().clear();
            if (dto.getSpecialties() != null) {
                user.getSpecialties().addAll(dto.getSpecialties());
            }
            userRepository.save(user);
        }
        
        return profileRepository.save(profile);
    }

    @Transactional
    public void updateProfileImage(Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() == Role.LAWYER) {
            LawyerProfile profile = getProfile(userId);
            profile.setProfileImageUrl(imageUrl);
            profileRepository.save(profile);
        } else if (user.getRole() == Role.CLIENT) {
            ClientProfile profile = getClientProfile(userId);
            profile.setProfileImageUrl(imageUrl);
            clientProfileRepository.save(profile);
        } else {
            throw new RuntimeException("Invalid user role for profile image update");
        }
    }
}
