package com.example.tech.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tech.model.Membership;
import com.example.tech.repository.MembershipRepository;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;

    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    // Get all memberships
    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    // Get membership by id
    public Optional<Membership> getMembershipById(Long id) {
        return membershipRepository.findById(id);
    }

    // Create membership
    public Membership createMembership(Membership membership) {
        return membershipRepository.save(membership);
    }

    // Update membership
    public Membership updateMembership(Long id, Membership updatedMembership) {
        return membershipRepository.findById(id)
                .map(membership -> {
                    updatedMembership.setMembershipId(id);
                    return membershipRepository.save(updatedMembership);
                })
                .orElseThrow(() ->
                        new RuntimeException("Membership not found with id " + id));
    }

    // Delete membership
    public void deleteMembership(Long id) {
        membershipRepository.deleteById(id);
    }
}