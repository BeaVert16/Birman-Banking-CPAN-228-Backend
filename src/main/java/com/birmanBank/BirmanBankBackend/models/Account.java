package com.birmanBank.BirmanBankBackend.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;
    @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits")
    private int cardNumber;
    private String password;
    private Role role;
}

enum Role {
    CUSTOMER,
    ADMIN
}