package com.example.userservice.controller;

import com.example.userservice.model.User;
import com.example.userservice.service.UserService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    @WithSpan("UserController.getAllUsers")
    public List<User> getAllUsers() {
        log.info("GET request received for all users");
        List<User> users = userService.getAllUsers();
        log.info("Returning {} users", users.size());
        return users;
    }
    
    @GetMapping("/{id}")
    @WithSpan("UserController.getUserById")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        log.info("GET request received for user with ID: {}", id);
        return userService.getUserById(id)
                .map(user -> {
                    log.info("Returning user: {}", user.getUsername());
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    log.warn("User not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }
    
    @GetMapping("/{id}/accounts")
    @WithSpan("UserController.getUserWithAccounts")
    public ResponseEntity<User> getUserWithAccounts(@PathVariable String id) {
        log.info("GET request received for user with accounts, ID: {}", id);
        return userService.getUserWithAccounts(id)
                .map(user -> {
                    log.info("Returning user with {} accounts: {}", 
                             user.getAccountIds().size(), 
                             user.getUsername());
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    log.warn("User not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }
}
