package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleId;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(length = 3000, nullable = false)
    private String description;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer likes;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer dislikes;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer commentsCount;

    @ManyToOne
    @JoinColumn(name = "primaryAuthor", nullable = false)
    private Blogger primaryAuthor;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer viewsCount;

    @Column(length = 255)
    private String postMedia;

    @Column
    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;


    // === Getters and Setters ===

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Integer getDislikes() {
        return dislikes;
    }

    public void setDislikes(Integer dislikes) {
        this.dislikes = dislikes;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    public Blogger getPrimaryAuthor() {
        return primaryAuthor;
    }

    public void setPrimaryAuthor(Blogger primaryAuthor) {
        this.primaryAuthor = primaryAuthor;
    }

    public Integer getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Integer viewsCount) {
        this.viewsCount = viewsCount;
    }

    public String getPostMedia() {
        return postMedia;
    }

    public void setPostMedia(String postMedia) {
        this.postMedia = postMedia;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
