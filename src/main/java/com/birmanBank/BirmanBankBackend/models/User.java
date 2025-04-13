package com.birmanBank.BirmanBankBackend.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String cardNumber; //unique identifier used for login
    private String password;  
    private String role; //admin or client

    //audit fields
    //log time for admin side and client side reasons
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}