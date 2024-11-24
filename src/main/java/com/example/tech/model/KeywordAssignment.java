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

}