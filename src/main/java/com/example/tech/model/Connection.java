package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Connection")
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long connectionId;

    @ManyToOne
    @JoinColumn(name = "followerId", nullable = false)
    private Blogger followerId; // to

    @ManyToOne
    @JoinColumn(name = "followingId", nullable = false)
    private Blogger followingId; // from

    @Column(name = "createdAt", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // === Getters and Setters ===

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public Blogger getFollowerId() {
        return followerId;
    }

    public void setFollowerId(Blogger followerId) {
        this.followerId = followerId;
    }

    public Blogger getFollowingId() {
        return followingId;
    }

    public void setFollowingId(Blogger followingId) {
        this.followingId = followingId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}