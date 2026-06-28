package com.lawapp.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "client_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientProfile {

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
}
