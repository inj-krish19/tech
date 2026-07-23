package com.example.tech.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import com.example.tech.model.Collaboration;
import com.example.tech.service.CollaborationService;

@RestController
@RequestMapping("/api/collaboration")
public class CollaborationController {

    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @GetMapping("/")
    public List<Collaboration> getAllCollaborations() {
        return collaborationService.getAllCollaborations();
    }

    @GetMapping("/{id}")
    public Optional<Collaboration> getCollaborationById(@PathVariable Long id) {
        return collaborationService.getCollaborationById(id);
    }

    @PostMapping("/")
    public Collaboration createCollaboration(@RequestBody Collaboration collaboration) {
        return collaborationService.createCollaboration(collaboration);
    }

    @PutMapping("/{id}")
    public Collaboration updateCollaboration(@PathVariable Long id,
                                             @RequestBody Collaboration collaboration) {
        return collaborationService.updateCollaboration(id, collaboration);
    }

    @DeleteMapping("/{id}")
    public void deleteCollaboration(@PathVariable Long id) {
        collaborationService.deleteCollaboration(id);
    }
}