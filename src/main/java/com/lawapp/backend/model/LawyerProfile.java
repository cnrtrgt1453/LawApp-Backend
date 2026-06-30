package com.lawapp.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lawyer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawyerProfile {

    @Id
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String profileImageUrl;
    private String youtubeUrl;
    
    private String linkedinUrl;
    private String instagramUrl;
    private String websiteUrl;
    private String city;

    public String getFullName() {
        return user != null ? user.getFullName() : null;
    }

    public java.util.Set<String> getSpecialties() {
        return user != null ? user.getSpecialties() : java.util.Collections.emptySet();
    }
}
