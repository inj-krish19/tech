package com.example.tech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tech.model.Collaboration;

@Repository
public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {

}