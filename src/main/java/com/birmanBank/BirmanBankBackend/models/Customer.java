package com.birmanBank.BirmanBankBackend.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "customers")
public class Customer {
    @Id
    private String id;
    @DBRef
    private Account account;
    private String name;
    private String address;
    private String contactDetails;
    private List<String> accounts; // List of Account IDs
    private Map<String, Double> balances; // AccountId -> Balance
    private List<String> transactions; // List of Transaction IDs
    // Other customer-specific fields
}