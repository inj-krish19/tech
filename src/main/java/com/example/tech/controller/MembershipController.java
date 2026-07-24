package com.example.tech.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import com.example.tech.model.Membership;
import com.example.tech.service.MembershipService;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/")
    public List<Membership> getAllMemberships() {
        return membershipService.getAllMemberships();
    }

    @GetMapping("/{id}")
    public Optional<Membership> getMembershipById(@PathVariable Long id) {
        return membershipService.getMembershipById(id);
    }

    @PostMapping("/")
    public Membership createMembership(@RequestBody Membership membership) {
        return membershipService.createMembership(membership);
    }

    @PutMapping("/{id}")
    public Membership updateMembership(@PathVariable Long id,
                                       @RequestBody Membership membership) {
        return membershipService.updateMembership(id, membership);
    }

    @DeleteMapping("/{id}")
    public void deleteMembership(@PathVariable Long id) {
        membershipService.deleteMembership(id);
    }
}