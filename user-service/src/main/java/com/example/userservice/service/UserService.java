package com.example.userservice.service;

import com.example.userservice.model.User;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {
    
    private final List<User> users = new ArrayList<>();
    private final RestTemplate restTemplate;
    private final Tracer tracer;
    
    @Autowired
    public UserService(RestTemplate restTemplate, Tracer tracer) {
        this.restTemplate = restTemplate;
        this.tracer = tracer;
        
        // Initialize with some sample user data with predefined IDs to match AccountService
        User user1 = new User("user1", "John", "Doe", "john.doe@example.com", "johndoe");
        User user2 = new User("user2", "Jane", "Smith", "jane.smith@example.com", "janesmith");
        User user3 = new User("user3", "Bob", "Johnson", "bob.johnson@example.com", "bjohnson");
        User user4 = new User("user4", "Sarah", "Williams", "sarah.williams@example.com", "swilliams");
        User user5 = new User("user5", "Robert", "Brown", "robert.brown@example.com", "rbrown");
        
        // Adding these users to our list
        users.add(user1);
        users.add(user2);
        users.add(user3);
        users.add(user4);
        users.add(user5);
        
        log.info("UserService initialized with {} users", users.size());
    }
    
    @WithSpan("UserService.getAllUsers")
    public List<User> getAllUsers() {
        log.info("Retrieving all users");
        Span span = Span.current();
        span.setAttribute("users.count", users.size());
        
        return users;
    }
    
    @WithSpan("UserService.getUserById")
    public Optional<User> getUserById(String id) {
        log.info("Finding user by ID: {}", id);
        Span span = Span.current();
        span.setAttribute("user.id", id);
        
        try {
            Optional<User> userOpt = users.stream()
                    .filter(user -> user.getId().equals(id))
                    .findFirst();
            
            if (userOpt.isPresent()) {
                span.setAttribute("user.found", true);
                log.info("User found: {}", userOpt.get().getUsername());
            } else {
                span.setAttribute("user.found", false);
                log.warn("User not found with ID: {}", id);
            }
            
            return userOpt;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            log.error("Error while finding user by ID: {}", id, e);
            throw e;
        }
    }
    
    /**
     * Get user with their accounts data loaded from account-service
     * 
     * @param userId the user ID to retrieve
     * @return Optional containing the user with accounts data, or empty if user not found
     */
    @WithSpan("UserService.getUserWithAccounts")
    public Optional<User> getUserWithAccounts(String userId) {
        log.info("Getting user with accounts, ID: {}", userId);
        Span span = Span.current();
        span.setAttribute("user.id", userId);
        
        Optional<User> userOpt = getUserById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            Span accountServiceSpan = tracer.spanBuilder("FetchAccounts")
                    .setParent(Context.current().with(Span.current()))
                    .startSpan();
            
            try {
                // Call account-service to get accounts for this user
                // In a production environment, you would use a more robust service discovery mechanism
                String accountServiceUrl = "http://localhost:8080/api/accounts/user/" + userId;
                accountServiceSpan.setAttribute("http.url", accountServiceUrl);
                accountServiceSpan.setAttribute("http.method", "GET");
                
                log.info("Calling account-service at URL: {}", accountServiceUrl);
                
                // This is a synchronous call. In production, consider using async communication
                // or circuit breakers to handle failures gracefully
                Object[] accounts = restTemplate.getForObject(accountServiceUrl, Object[].class);
                
                if (accounts != null) {
                    // Extract account IDs and set them in the user object
                    List<String> accountIds = new ArrayList<>();
                    for (Object account : accounts) {
                        if (account instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> accountMap = (java.util.Map<String, Object>) account;
                            if (accountMap.containsKey("id")) {
                                accountIds.add(accountMap.get("id").toString());
                            }
                        }
                    }
                    user.setAccountIds(accountIds);
                    
                    span.setAttribute("accounts.count", accountIds.size());
                    accountServiceSpan.setAttribute("accounts.count", accountIds.size());
                    
                    log.info("Retrieved {} accounts for user {}", accountIds.size(), userId);
                } else {
                    log.warn("No accounts returned from account-service for user: {}", userId);
                    span.setAttribute("accounts.count", 0);
                    accountServiceSpan.setAttribute("accounts.count", 0);
                }
                
                accountServiceSpan.setStatus(StatusCode.OK);
            } catch (Exception e) {
                // Log the error but continue - this provides resilience if the account-service is down
                log.error("Error fetching accounts for user {}: {}", userId, e.getMessage(), e);
                
                // Record the error in the span
                accountServiceSpan.recordException(e);
                accountServiceSpan.setStatus(StatusCode.ERROR, "Failed to fetch accounts: " + e.getMessage());
                
                span.setAttribute("error", true);
                span.setAttribute("error.message", e.getMessage());
            } finally {
                accountServiceSpan.end();
            }
            
            return Optional.of(user);
        }
        
        return Optional.empty();
    }
}
