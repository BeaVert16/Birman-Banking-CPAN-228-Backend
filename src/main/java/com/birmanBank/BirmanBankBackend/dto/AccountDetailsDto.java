package com.birmanBank.BirmanBankBackend.dto;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;


/* 
* this class is used to send account details and transactions to the client
* it contains the account and a page of transactions
* the page of transactions is used to implement pagination
* the account is used to display account details
*/


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailsDto {
    private Account account;
    private Page<Transaction> transactions;
}