package com.example.tech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tech.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

}