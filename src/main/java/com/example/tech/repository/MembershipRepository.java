package com.example.tech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tech.model.Membership;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

}