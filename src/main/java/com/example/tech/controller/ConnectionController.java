package com.example.tech.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import com.example.tech.model.Connection;
import com.example.tech.service.ConnectionService;

@RestController
@RequestMapping("/api/connection")
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @GetMapping("/")
    public List<Connection> getAllConnections() {
        return connectionService.getAllConnections();
    }

    @GetMapping("/{id}")
    public Optional<Connection> getConnectionById(@PathVariable Long id) {
        return connectionService.getConnectionById(id);
    }

    @PostMapping("/")
    public Connection createConnection(@RequestBody Connection connection) {
        return connectionService.createConnection(connection);
    }

    @PutMapping("/{id}")
    public Connection updateConnection(@PathVariable Long id,
                                       @RequestBody Connection connection) {
        return connectionService.updateConnection(id, connection);
    }

    @DeleteMapping("/{id}")
    public void deleteConnection(@PathVariable Long id) {
        connectionService.deleteConnection(id);
    }
}