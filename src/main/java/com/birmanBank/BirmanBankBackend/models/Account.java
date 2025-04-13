package com.birmanBank.BirmanBankBackend.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "accounts")
public class Account {
    @Id
    private String accountId;

    //foreign key linking to the Client's clientId.
    //ensures that each account belongs to one and only one client.
    private String clientId;  

    private String accountName;
    private String accountType;
    private BigDecimal balance; //current balance of the account
    private String status; //active, inactive

    //log time for admin side and client side reason
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}