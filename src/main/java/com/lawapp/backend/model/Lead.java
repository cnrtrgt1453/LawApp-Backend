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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
