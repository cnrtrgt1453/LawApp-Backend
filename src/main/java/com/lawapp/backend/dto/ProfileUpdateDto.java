package com.lawapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateDto {
    private String fullName;
    private String bio;
    private String linkedinUrl;
    private String instagramUrl;
    private String websiteUrl;
    private String youtubeUrl;
    private String city;
    private java.util.List<String> specialties;
}
