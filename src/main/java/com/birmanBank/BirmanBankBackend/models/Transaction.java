package com.birmanBank.BirmanBankBackend.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String transactionId;
    private String transactionType;
    private double amount;
    private LocalDateTime timestamp;
    private String accountId; // Reference to the account involved.
    private String sourceAccountId; // For transfers.
    private String destinationAccountId; // For transfers.
    private String description;
}
