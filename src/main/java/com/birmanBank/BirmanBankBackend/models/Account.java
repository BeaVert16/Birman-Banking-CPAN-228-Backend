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

    private String clientId;  

    private String accountName;
    private String accountType;
    private BigDecimal balance; 
    private String status; 

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}