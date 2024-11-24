package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PostCategoryAssignment")
public class PostCategoryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postCategoryAssignmentId;

    @ManyToOne
    @JoinColumn(name = "articleId", nullable = false)
    private Post article;

    @ManyToOne
    @JoinColumn(name = "categoryId", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "assignedBy", nullable = false)
    private Blogger assignedBy;

    @Column(name = "createdAt", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}