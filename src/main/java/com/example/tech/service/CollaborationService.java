package com.example.tech.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tech.model.Collaboration;
import com.example.tech.repository.CollaborationRepository;

@Service
public class CollaborationService {

    private final CollaborationRepository collaborationRepository;

    public CollaborationService(CollaborationRepository collaborationRepository) {
        this.collaborationRepository = collaborationRepository;
    }

    // Get all collaborations
    public List<Collaboration> getAllCollaborations() {
        return collaborationRepository.findAll();
    }

    // Get collaboration by id
    public Optional<Collaboration> getCollaborationById(Long id) {
        return collaborationRepository.findById(id);
    }

    // Create collaboration
    public Collaboration createCollaboration(Collaboration collaboration) {
        return collaborationRepository.save(collaboration);
    }

    // Update collaboration
    public Collaboration updateCollaboration(Long id, Collaboration updatedCollaboration) {
        return collaborationRepository.findById(id)
                .map(collaboration -> {
                    updatedCollaboration.setCollaborationId(id);
                    return collaborationRepository.save(updatedCollaboration);
                })
                .orElseThrow(() ->
                        new RuntimeException("Collaboration not found with id " + id));
    }

    // Delete collaboration
    public void deleteCollaboration(Long id) {
        collaborationRepository.deleteById(id);
    }
}