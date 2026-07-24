package com.example.tech.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tech.model.KeywordAssignment;
import com.example.tech.repository.KeywordAssignmentRepository;

@Service
public class KeywordAssignmentService {

    private final KeywordAssignmentRepository keywordAssignmentRepository;

    public KeywordAssignmentService(KeywordAssignmentRepository keywordAssignmentRepository) {
        this.keywordAssignmentRepository = keywordAssignmentRepository;
    }

    // Get all keyword assignments
    public List<KeywordAssignment> getAllKeywordAssignments() {
        return keywordAssignmentRepository.findAll();
    }

    // Get keyword assignment by id
    public Optional<KeywordAssignment> getKeywordAssignmentById(Long id) {
        return keywordAssignmentRepository.findById(id);
    }

    // Create keyword assignment
    public KeywordAssignment createKeywordAssignment(KeywordAssignment keywordAssignment) {
        return keywordAssignmentRepository.save(keywordAssignment);
    }

    // Update keyword assignment
    public KeywordAssignment updateKeywordAssignment(Long id, KeywordAssignment updatedKeywordAssignment) {
        return keywordAssignmentRepository.findById(id)
                .map(keywordAssignment -> {
                    updatedKeywordAssignment.setKeywordAssignmentId(id);
                    return keywordAssignmentRepository.save(updatedKeywordAssignment);
                })
                .orElseThrow(() ->
                        new RuntimeException("Keyword Assignment not found with id " + id));
    }

    // Delete keyword assignment
    public void deleteKeywordAssignment(Long id) {
        keywordAssignmentRepository.deleteById(id);
    }
}