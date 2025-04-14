package com.birmanBank.BirmanBankBackend.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "clients")
public class Client {
    @Id
    private String clientId; // Unique identifier for the client
    private String userCardNumber; // Card number assigned to the client
    private Boolean Activated = true;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private Address address;
    private String sin;
    private String dateOfBirth;

    // Log time for admin and client-side purposes
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}