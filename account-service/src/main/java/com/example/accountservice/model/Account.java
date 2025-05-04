package com.example.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private String id;
    private String name;
    private String accountNumber;
    private BigDecimal balance;
    private String userId;
    
    // Constructor without userId for backward compatibility
    public Account(String id, String name, String accountNumber, BigDecimal balance) {
        this.id = id;
        this.name = name;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }
}
