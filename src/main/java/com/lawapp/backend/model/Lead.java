package com.lawapp.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String category; // Örn: Boşanma, Ceza, İş Hukuku

    @Column(nullable = false)
    private String city;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status = LeadStatus.OPEN;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(columnDefinition = "TEXT")
    private String wizardAnswersJson; // Sihirbaz sorularının JSON formatındaki yanıtları

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        maskSensitiveData();
    }

    @PreUpdate
    protected void onUpdate() {
        maskSensitiveData();
    }

    private void maskSensitiveData() {
        if (description != null) {
            // Telefon numaralarını maskele (Örn: 0532 123 45 67 veya 5321234567)
            description = description.replaceAll("(?i)\\b(0?\\s?[5-9]\\d{2}\\s?\\d{3}\\s?\\d{2}\\s?\\d{2})\\b", "[TELEFON MASKELENDİ]");
            
            // TCKN maskele (11 haneli rakamlar)
            description = description.replaceAll("\\b\\d{11}\\b", "[TCKN MASKELENDİ]");
            
            // E-posta adreslerini maskele
            description = description.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[E-POSTA MASKELENDİ]");
        }
    }
}
