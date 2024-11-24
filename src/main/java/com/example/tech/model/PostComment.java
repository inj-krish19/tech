package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PostComment")
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postCommentId;

    @ManyToOne
    @JoinColumn(name = "articleId", nullable = false)
    private Post article;

    @ManyToOne
    @JoinColumn(name = "authorId", nullable = false)
    private Blogger author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @ManyToOne
    @JoinColumn(name = "parentCommentId")
    private PostComment parentComment;

    @Column(name = "createdAt", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}