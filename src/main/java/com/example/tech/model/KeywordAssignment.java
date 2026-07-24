package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "KeywordAssignment")
public class KeywordAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long keywordAssignmentId;

    @ManyToOne
    @JoinColumn(name = "articleId", nullable = false)
    private Post article;

    @ManyToOne
    @JoinColumn(name = "keywordId", nullable = false)
    private Keyword keyword;

    @ManyToOne
    @JoinColumn(name = "assignedBy", nullable = false)
    private Blogger assignedBy;

    @Column(name = "createdAt", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // === Getters and Setters ===

    public Long getKeywordAssignmentId() {
        return keywordAssignmentId;
    }

    public void setKeywordAssignmentId(Long keywordAssignmentId) {
        this.keywordAssignmentId = keywordAssignmentId;
    }

    public Post getArticle() {
        return article;
    }

    public void setArticle(Post article) {
        this.article = article;
    }

    public Keyword getKeyword() {
        return keyword;
    }

    public void setKeyword(Keyword keyword) {
        this.keyword = keyword;
    }

    public Blogger getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Blogger assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}