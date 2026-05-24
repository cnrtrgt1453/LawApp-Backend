package com.lawapp.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(precision = 10, scale = 2)
    private BigDecimal creditBalance; // Sadece avukatlar için kredi, müvekkiller için 0 olabilir.

    private String phoneNumber;
    
    private boolean verified; // Baro levha doğrulaması veya telefon doğrulaması için

    @Column(unique = true)
    private String barNumber; // Baro sicil numarası - sadece avukatlar için

    private String barLicenseImageUrl; // Avukat ruhsat belgesi görseli
    
    private Double averageRating; // Ortalama değerlendirme puanı

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_specialties", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "specialty")
    private java.util.Set<String> specialties = new java.util.HashSet<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String fcmToken; // FCM Push Notification Token

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private LawyerProfile lawyerProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private ClientProfile clientProfile;
}
