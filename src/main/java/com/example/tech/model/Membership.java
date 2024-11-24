package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Membership")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long membershipId;

    @ManyToOne
    @JoinColumn(name = "communityId", nullable = false)
    private Community group;

    @ManyToOne
    @JoinColumn(name = "authorId", nullable = false)
    private Blogger author;

    @Column(name = "joinedAt", updatable = false, nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

}