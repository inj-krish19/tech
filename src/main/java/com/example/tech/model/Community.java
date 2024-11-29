package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Community")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long communityId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 1000, nullable = false)
    private String communityDescription;

    @Column(length = 255)
    private String communityIcon;

    @ManyToOne
    @JoinColumn(name = "createdBy", nullable = false)
    private Blogger createdBy;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

}