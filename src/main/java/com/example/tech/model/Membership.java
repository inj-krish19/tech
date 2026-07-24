package com.example.tech.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Membership")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long membershipId;

    @ManyToOne
    @JoinColumn(name = "communityId", nullable = false)
    private Community group;

    @ManyToOne
    @JoinColumn(name = "authorId", nullable = false)
    private Blogger author;

    @Column(name = "joinedAt", updatable = false, nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    // === Getters and Setters ===

    public Long getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Long membershipId) {
        this.membershipId = membershipId;
    }

    public Community getGroup() {
        return group;
    }

    public void setGroup(Community group) {
        this.group = group;
    }

    public Blogger getAuthor() {
        return author;
    }

    public void setAuthor(Blogger author) {
        this.author = author;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}