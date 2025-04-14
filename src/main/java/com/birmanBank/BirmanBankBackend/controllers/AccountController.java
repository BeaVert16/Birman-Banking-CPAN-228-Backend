package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Transaction;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.services.AuthenticationService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.AccountService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/*
 * accountController handles requests related to user accounts
 * provides endpoints for retrieving, creating, updating, and deleting accounts
 */

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    //-----------------------Constructors----------------------//
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuthenticationService authUserDetailsService;

    public AccountController(AccountService accountService, TransactionService transactionService,
            AuthenticationService authUserDetailsService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.authUserDetailsService = authUserDetailsService;
    }
    // ---------------------------------------------------------------//

    // endpoint to get all accounts for the authenticated user
    @GetMapping
    public ResponseEntity<List<Account>> getUserAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        String cardNumber = authUserDetailsService.validateAuthenticatedUser(userDetails); // validate the authenticated user
        List<Account> accounts = accountService.getAccountsForAuthenticatedUser(cardNumber); // get accounts for the authenticated user
        return ResponseEntity.ok(accounts); // return the list of accounts
    }

    // retrieve basic information about a specific account
    @GetMapping("/{accountId}/basic")
    public ResponseEntity<Account> getAccountBasicInfo(@PathVariable String accountId,
            @AuthenticationPrincipal UserDetails userDetails) { //
        String cardNumber = authUserDetailsService.validateAuthenticatedUser(userDetails); // get the authenticated client
        authUserDetailsService.verifyAccountOwnership(accountId, cardNumber); // verify ownership of the account

        // retrieve the account by ID
        Account account = accountService.getAccountById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")); // handle account is not found case
        return ResponseEntity.ok(account); // return the account information
    }

    // endpoint to get detailed information about a specific account, including transactions
    // uses pagnation
    @GetMapping("/{accountId}/details")
    public ResponseEntity<AccountDetailsDto> getAccountDetails(
            @PathVariable String accountId,
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {

        String cardNumber = authUserDetailsService.validateAuthenticatedUser(userDetails); // validate the authenticated user
        authUserDetailsService.verifyAccountOwnership(accountId, cardNumber); // verify ownership of the account

        // retrieve the account by ID
        Account account = accountService.getAccountById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")); // handle account is not found case

        // retrieve transactions for the account using pagination
        Page<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId, pageable); 

        // create a response DTO with account and transactions 
        AccountDetailsDto responseDto = new AccountDetailsDto(account, transactions); 
        return ResponseEntity.ok(responseDto);
    }

    // endpoint to update the name of a specific account
    @PutMapping("/{accountId}/update-name")
    public ResponseEntity<Account> updateAccountName(
            @PathVariable String accountId,
            @RequestBody Map<String, String> requestBody,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String cardNumber = authUserDetailsService.validateAuthenticatedUser(userDetails); // validate the authenticated user
        Client client = authUserDetailsService.getAuthenticatedClient(cardNumber); 
        String newAccountName = requestBody.get("accountName"); // get the new account name from the request body (frontend)

        // check if the new account name is provided
        // if not, throw a bad request exception
        if (newAccountName == null || newAccountName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New account name is required.");
        }

        Account updatedAccount = accountService.updateAccountName(accountId, client.getClientId(), newAccountName); // update the account name

        return ResponseEntity.ok(updatedAccount);
    }

    // endpoint to create a new account for the authenticated user
    @PostMapping("/create")
    public ResponseEntity<Account> createAccount(
            @RequestBody Map<String, String> accountRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        String cardNumber = authUserDetailsService.validateAuthenticatedUser(userDetails); // validate the authenticated user
        Client client = authUserDetailsService.getAuthenticatedClient(cardNumber); // get the authenticated clien

        // extract account details from the request body
        String accountName = accountRequest.get("accountName");
        String accountType = accountRequest.get("accountType");

        // check if the account name is provided
        // if not throw a bad request exception
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account name is required.");
        }

        // check if the account type is provided
        Account createdAccount = accountService.createAndAttachAccount(client.getClientId(), accountName, accountType);


        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount); // returns the created account with status
    }

    // endpoint to delete a specific account
    @DeleteMapping("/{accountId}/delete")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable String accountId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String cardNumber = authUserDetailsService.validateAuthenticatedUser(userDetails); // validate the authenticated user
        Client client = authUserDetailsService.getAuthenticatedClient(cardNumber); // get the authenticated client

        // delete the account
        accountService.deleteAccount(accountId, client.getClientId());

        // return a no content response indicating successful deletion, no actual return
        return ResponseEntity.noContent().build(); 
    }
}