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
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String transactionId; // Unique primary key for each transaction

    //foreign key referencing the Account this transaction belongs to.
    //ensures that each transaction is linked to one account only.
    private String accountId;  

    private String transactionType;  
    private BigDecimal transactionAmount;  
    private BigDecimal postTransactionBalance;

    private LocalDateTime timestamp; //date and time of the transaction

    //used when the transaction is a transfer to another account.
    private String transferToAccountId;  

    private String description;
}
