package com.example.tech.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tech.model.Community;
import com.example.tech.repository.CommunityRepository;

@Service
public class CommunityService {

    private final CommunityRepository communityRepository;

    public CommunityService(CommunityRepository communityRepository) {
        this.communityRepository = communityRepository;
    }

    // Get all communities
    public List<Community> getAllCommunities() {
        return communityRepository.findAll();
    }

    // Get community by id
    public Optional<Community> getCommunityById(Long id) {
        return communityRepository.findById(id);
    }

    // Create community
    public Community createCommunity(Community community) {
        return communityRepository.save(community);
    }

    // Update community
    public Community updateCommunity(Long id, Community updatedCommunity) {
        return communityRepository.findById(id)
                .map(community -> {
                    updatedCommunity.setCommunityId(id);
                    return communityRepository.save(updatedCommunity);
                })
                .orElseThrow(() ->
                        new RuntimeException("Community not found with id " + id));
    }

    // Delete community
    public void deleteCommunity(Long id) {
        communityRepository.deleteById(id);
    }
}