package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Blogger")
public class Blogger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long authorId;

    @Column(unique = true,length = 20, nullable = false)
    private String username;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 250)
    private String bio;

    @Column(length = 255)
    private String profilePicture;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

}