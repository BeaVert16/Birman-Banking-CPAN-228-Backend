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
@Document(collection = "loans")
public class Loan {
    @Id
    private String loanId; // loan's unique identifier.
    private String userCardNumber; // account that requested the loan.
    private BigDecimal amountRequested; // amount requested.
    private BigDecimal amountOutstanding; // amount outstanding.
    private String status; // loan status (pending/denied/awaiting/paid/).
    private LocalDateTime loanDate; // date loan was requested.
    private LocalDateTime decidedAt; // when admin interacted with loan.
    private String decisionByAdmin; // admin that interacted with loan.
}
