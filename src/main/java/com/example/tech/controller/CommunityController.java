package com.example.tech.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import com.example.tech.model.Community;
import com.example.tech.service.CommunityService;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/")
    public List<Community> getAllCommunities() {
        return communityService.getAllCommunities();
    }

    @GetMapping("/{id}")
    public Optional<Community> getCommunityById(@PathVariable Long id) {
        return communityService.getCommunityById(id);
    }

    @PostMapping("/")
    public Community createCommunity(@RequestBody Community community) {
        return communityService.createCommunity(community);
    }

    @PutMapping("/{id}")
    public Community updateCommunity(@PathVariable Long id,
                                     @RequestBody Community community) {
        return communityService.updateCommunity(id, community);
    }

    @DeleteMapping("/{id}")
    public void deleteCommunity(@PathVariable Long id) {
        return;
        // or simply:
        // communityService.deleteCommunity(id);
    }
}