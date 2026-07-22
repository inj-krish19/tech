package com.example.tech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tech.model.Blogger;

@Repository
public interface BloggerRepository extends JpaRepository<Blogger, Long> {

}