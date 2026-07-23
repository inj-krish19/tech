package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Collaboration")
public class Collaboration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long collaborationId;

    @ManyToOne
    @JoinColumn(name = "articleId", nullable = false)
    private Post article;

    @ManyToOne
    @JoinColumn(name = "authorId", nullable = false)
    private Blogger author;

    @Column(name = "createdAt", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // === Getters and Setters ===

    public Long getCollaborationId() {
        return collaborationId;
    }

    public void setCollaborationId(Long collaborationId) {
        this.collaborationId = collaborationId;
    }

    public Post getArticle() {
        return article;
    }

    public void setArticle(Post article) {
        this.article = article;
    }

    public Blogger getAuthor() {
        return author;
    }

    public void setAuthor(Blogger author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}