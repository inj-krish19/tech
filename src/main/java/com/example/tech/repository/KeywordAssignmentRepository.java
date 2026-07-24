package com.example.tech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tech.model.KeywordAssignment;

@Repository
public interface KeywordAssignmentRepository extends JpaRepository<KeywordAssignment, Long> {

}