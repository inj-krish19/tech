package com.example.tech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tech.model.Community;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

}