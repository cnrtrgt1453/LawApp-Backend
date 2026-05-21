package com.lawapp.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bid_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "lawyer_id", nullable = false)
    private User lawyer;
}
