package com.birmanBank.BirmanBankBackend.controllers.ClientControllers;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Transaction;
import com.birmanBank.BirmanBankBackend.services.ClientServices.AccountService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.ClientService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.TransactionService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final ClientService clientService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, ClientService clientService, TransactionService transactionService) {
        this.accountService = accountService;
        this.clientService = clientService;
        this.transactionService = transactionService;
    }

    // Endpoint to fetch basic account details for the authenticated user
    @GetMapping("/{accountId}/basic")
    public Map<String, Object> getBasicAccountDetails(@PathVariable String accountId, Authentication authentication) {
        String cardNumber = authentication.getName(); // Extract card number from JWT
        Account account = accountService.getAccountById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        // Ensure the authenticated user owns the account
        String clientId = account.getClientId();
        clientService.getClientById(clientId).ifPresentOrElse(client -> {
            if (!client.getUserCardNumber().equals(cardNumber)) {
                throw new RuntimeException("Unauthorized access to account");
            }
        }, () -> {
            throw new RuntimeException("Client not found");
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", account.getStatus());
        response.put("balance", account.getBalance());
        response.put("accountType", account.getAccountType());
        return response;
    }

    // Endpoint to fetch detailed account information for a specific account
    @GetMapping("/{accountId}/details")
    public Map<String, Object> getAccountDetails(@PathVariable String accountId, Authentication authentication) {
        String cardNumber = authentication.getName(); // Extract card number from JWT
        Account account = accountService.getAccountById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        // Ensure the authenticated user owns the account
        String clientId = account.getClientId();
        clientService.getClientById(clientId).ifPresentOrElse(client -> {
            if (!client.getUserCardNumber().equals(cardNumber)) {
                throw new RuntimeException("Unauthorized access to account");
            }
        }, () -> {
            throw new RuntimeException("Client not found");
        });

        // Fetch transactions for the account
        List<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId);

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", account.getAccountId());
        response.put("accountType", account.getAccountType());
        response.put("balance", account.getBalance());
        response.put("status", account.getStatus());
        response.put("transactions", transactions);
        return response;
    }
}