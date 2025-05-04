package com.example.accountservice.service;

import com.example.accountservice.model.Account;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountService {

    private final List<Account> accounts = new ArrayList<>();
    private final Tracer tracer;

    @Autowired
    public AccountService(Tracer tracer) {
        this.tracer = tracer;
        
        // Initialize with some sample account data with user IDs
        // User 1 accounts
        String user1Id = "user1";
        accounts.add(new Account(UUID.randomUUID().toString(), "John Doe", "ACC001", BigDecimal.valueOf(1000.00), user1Id));
        accounts.add(new Account(UUID.randomUUID().toString(), "John Doe", "ACC004", BigDecimal.valueOf(500.75), user1Id));
        
        // User 2 accounts
        String user2Id = "user2";
        accounts.add(new Account(UUID.randomUUID().toString(), "Jane Smith", "ACC002", BigDecimal.valueOf(2500.50), user2Id));
        
        // User 3 accounts
        String user3Id = "user3";
        accounts.add(new Account(UUID.randomUUID().toString(), "Bob Johnson", "ACC003", BigDecimal.valueOf(750.25), user3Id));
        accounts.add(new Account(UUID.randomUUID().toString(), "Bob Johnson", "ACC005", BigDecimal.valueOf(1250.60), user3Id));
        
        log.info("AccountService initialized with {} accounts", accounts.size());
    }

    @WithSpan("AccountService.getAllAccounts")
    public List<Account> getAllAccounts() {
        log.info("Retrieving all accounts");
        Span span = Span.current();
        span.setAttribute("accounts.count", accounts.size());
        
        // If random balances are enabled, update balances before returning
        return accounts;
    }
    
    /**
     * Find an account by its ID
     * 
     * @param id the account ID to search for
     * @return an Optional containing the account if found, or empty if not found
     */
    @WithSpan("AccountService.getAccountById")
    public java.util.Optional<Account> getAccountById(String id) {
        log.info("Finding account by ID: {}", id);
        Span span = Span.current();
        span.setAttribute("account.id", id);
        
        try {
            java.util.Optional<Account> accountOpt = accounts.stream()
                    .filter(account -> account.getId().equals(id))
                    .findFirst();
            
            if (accountOpt.isPresent()) {
                span.setAttribute("account.found", true);
                log.info("Account found: {}", accountOpt.get().getAccountNumber());
            } else {
                span.setAttribute("account.found", false);
                log.warn("Account not found with ID: {}", id);
            }
            
            return accountOpt;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            log.error("Error while finding account by ID: {}", id, e);
            throw e;
        }
    }
    
    /**
     * Find all accounts for a specific user
     * 
     * @param userId the user ID to search for
     * @return a list of accounts belonging to the user
     */
    @WithSpan("AccountService.getAccountsByUserId")
    public List<Account> getAccountsByUserId(String userId) {
        log.info("Finding accounts for user ID: {}", userId);
        
        Span span = Span.current();
        span.setAttribute("user.id", userId);
        
        try {
            List<Account> userAccounts = accounts.stream()
                    .filter(account -> userId.equals(account.getUserId()))
                    .collect(Collectors.toList());
            
            span.setAttribute("accounts.count", userAccounts.size());
            log.info("Found {} accounts for user ID: {}", userAccounts.size(), userId);
            
            return userAccounts;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            log.error("Error while finding accounts for user ID: {}", userId, e);
            throw e;
        }
    }
}
