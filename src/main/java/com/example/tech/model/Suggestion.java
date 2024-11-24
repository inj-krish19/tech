package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Suggestion")
public class Suggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long suggestionId;

    @ManyToOne
    @JoinColumn(name = "authorId", nullable = false)
    private Blogger author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "createdAt", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}