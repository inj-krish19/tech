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

    @Column(columnDefinition = "TEXT", nullable = false)
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
    
}
