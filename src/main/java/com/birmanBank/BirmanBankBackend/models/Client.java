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
    private String clientId; //unique identifier for the client

    //foreign key referencing the Users table's cardNumber
    private String userCardNumber; 

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String address;
    private String sin; // Social Insurance Number

    //log time for admin side and client side reasons
    private LocalDateTime createdAt;
}
