package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Keyword")
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long keywordId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String keywordDescription;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    private LocalDateTime createdAt;

}