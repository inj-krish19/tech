package com.example.tech.repository;

import com.example.tech.model.Blogger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BloggerRepository extends JpaRepository<Blogger, Long> {
    // Add custom query methods if needed
}
