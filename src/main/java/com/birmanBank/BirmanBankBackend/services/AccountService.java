package com.birmanBank.BirmanBankBackend.services;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.repositories.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Create a new account with validation.
    public Account createAccount(Account account) {
        validateCardNumber(account.getCardNumber());
        return accountRepository.save(account);
    }

    // Validate that the card number is exactly 16-digits.
    private void validateCardNumber(int cardNumber) {
        String cardNumberStr = String.valueOf(cardNumber);
        if (cardNumberStr.length() != 16 || !cardNumberStr.matches("\\d{16}")) {
            throw new IllegalArgumentException("Card number must be exactly 16 digits");
        }
    }
}