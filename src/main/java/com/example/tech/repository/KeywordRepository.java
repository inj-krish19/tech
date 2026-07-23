package com.example.tech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tech.model.Keyword;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

}