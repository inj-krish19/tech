package com.example.tech.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import com.example.tech.model.KeywordAssignment;
import com.example.tech.service.KeywordAssignmentService;

@RestController
@RequestMapping("/api/keyword-assignment")
public class KeywordAssignmentController {

    private final KeywordAssignmentService keywordAssignmentService;

    public KeywordAssignmentController(KeywordAssignmentService keywordAssignmentService) {
        this.keywordAssignmentService = keywordAssignmentService;
    }

    @GetMapping("/")
    public List<KeywordAssignment> getAllKeywordAssignments() {
        return keywordAssignmentService.getAllKeywordAssignments();
    }

    @GetMapping("/{id}")
    public Optional<KeywordAssignment> getKeywordAssignmentById(@PathVariable Long id) {
        return keywordAssignmentService.getKeywordAssignmentById(id);
    }

    @PostMapping("/")
    public KeywordAssignment createKeywordAssignment(@RequestBody KeywordAssignment keywordAssignment) {
        return keywordAssignmentService.createKeywordAssignment(keywordAssignment);
    }

    @PutMapping("/{id}")
    public KeywordAssignment updateKeywordAssignment(@PathVariable Long id,
                                                     @RequestBody KeywordAssignment keywordAssignment) {
        return keywordAssignmentService.updateKeywordAssignment(id, keywordAssignment);
    }

    @DeleteMapping("/{id}")
    public void deleteKeywordAssignment(@PathVariable Long id) {
        keywordAssignmentService.deleteKeywordAssignment(id);
    }
}