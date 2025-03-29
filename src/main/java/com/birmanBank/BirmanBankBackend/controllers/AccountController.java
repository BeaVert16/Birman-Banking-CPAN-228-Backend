package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.repositories.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    //// Get account by card number
    // @GetMapping("/{cardNumber}")
    // public ResponseEntity<Account> getAccountByCardNumber(@PathVariable int cardNumber) {
    //     Account account = accountRepository.findByCardNumber(cardNumber);
    //     if (account == null) {
    //         return ResponseEntity.status(404).body(null);
    //     }
    //     return ResponseEntity.ok(account);
    // }
    //// Get all accounts
    // @GetMapping ("/")
    // public List<Account> getAllAccounts() {
    //     return accountRepository.findAll();
    // }

    // Create a new account
    @PostMapping ("/register")
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        Account savedAccount = accountRepository.save(account);
        return ResponseEntity.ok(savedAccount);
    }
}