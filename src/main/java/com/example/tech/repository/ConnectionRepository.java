package com.example.tech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.tech.model.Connection;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

}