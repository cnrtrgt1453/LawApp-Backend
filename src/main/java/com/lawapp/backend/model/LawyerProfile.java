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

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String profileImageUrl;
    private String introVideoUrl;
    
    private String linkedinUrl;
    private String instagramUrl;
    private String websiteUrl;
}
