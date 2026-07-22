package com.example.tech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tech.model.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

}