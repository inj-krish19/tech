package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PostInteraction")
public class PostInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postReactionId;

    @ManyToOne
    @JoinColumn(name = "articleId", nullable = false)
    private Post article;

    @ManyToOne
    @JoinColumn(name = "authorId", nullable = false)
    private Blogger author;

    @Column(name = "createdAt", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}