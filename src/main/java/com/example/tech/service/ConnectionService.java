package com.example.tech.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tech.model.Connection;
import com.example.tech.repository.ConnectionRepository;

@Service
public class ConnectionService {

    private final ConnectionRepository connectionRepository;

    public ConnectionService(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    // Get all connections
    public List<Connection> getAllConnections() {
        return connectionRepository.findAll();
    }

    // Get connection by id
    public Optional<Connection> getConnectionById(Long id) {
        return connectionRepository.findById(id);
    }

    // Create connection
    public Connection createConnection(Connection connection) {
        return connectionRepository.save(connection);
    }

    // Update connection
    public Connection updateConnection(Long id, Connection updatedConnection) {
        return connectionRepository.findById(id)
                .map(connection -> {
                    updatedConnection.setConnectionId(id);
                    return connectionRepository.save(updatedConnection);
                })
                .orElseThrow(() ->
                        new RuntimeException("Connection not found with id " + id));
    }

    // Delete connection
    public void deleteConnection(Long id) {
        connectionRepository.deleteById(id);
    }
}