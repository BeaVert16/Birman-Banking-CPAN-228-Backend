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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;

import java.util.List;
import java.util.Map;

/*
 * accountController handles requests related to user accounts
 * provides endpoints for retrieving, creating, updating, and deleting accounts
 */

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    // -----------------------Constructors----------------------//
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuthenticationService authenticationService;

    public AccountController(AccountService accountService, TransactionService transactionService,
            AuthenticationService authenticationService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.authenticationService = authenticationService;
    }
    // ---------------------------------------------------------------//

    // endpoint to get all accounts for the authenticated user
    @GetMapping
    public ResponseEntity<List<Account>> getUserAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        String cardNumber = authenticationService.validateAuthenticatedUser(userDetails); // validate the authenticated user
        List<Account> accounts = accountService.getAccountsForAuthenticatedUser(cardNumber); // get accounts for the authenticated user
        return ResponseEntity.ok(accounts); // return the list of accounts
    }

    // retrieve basic information about a specific account
    @GetMapping("/{accountId}/basic")
    public ResponseEntity<Account> getAccountBasicInfo(@PathVariable String accountId,
            @AuthenticationPrincipal UserDetails userDetails) { //

        authenticationService.validateUserAndAccountOwnership(userDetails, accountId);

        // retrieve the account by ID
        Account account = accountService.getAccountById(accountId)
                // handle account is not found case
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        return ResponseEntity.ok(account); // return the account information
    }

    // endpoint to get detailed information about a specific account, including
    // transactions
    @GetMapping("/{accountId}/details")
    public ResponseEntity<PagedModel<EntityModel<Transaction>>> getAccountDetails(
            @PathVariable String accountId,
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable,
            PagedResourcesAssembler<Transaction> pagedResourcesAssembler) {

        String cardNumber = authenticationService.validateAuthenticatedUser(userDetails); // validate the authenticated user
        authenticationService.verifyAccountOwnership(accountId, cardNumber); // verify ownership of the account

        accountService.getAccountById(accountId)
                // handle account is not found case
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Page<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId, pageable);

        PagedModel<EntityModel<Transaction>> pagedModel = pagedResourcesAssembler.toModel(transactions);

        return ResponseEntity.ok(pagedModel);
    }

    // endpoint to update the name of a specific account
    @PutMapping("/{accountId}/update-name")
    public ResponseEntity<Account> updateAccountName(
            @PathVariable String accountId,
            @RequestBody Map<String, String> requestBody,
            @AuthenticationPrincipal UserDetails userDetails) {

        authenticationService.validateUserAndAccountOwnership(userDetails, accountId);
        Client client = authenticationService.getAuthenticatedClient(userDetails); // Fetch the Client object
        String newAccountName = requestBody.get("accountName"); // get the new account name from the request body

        // update the account name
        Account updatedAccount = accountService.updateAccountName(accountId, client.getClientId(), newAccountName);

        return ResponseEntity.ok(updatedAccount);
    }

    // endpoint to create a new account for the authenticated user
    @PostMapping("/create")
    public ResponseEntity<Account> createAccount(
            @RequestBody Map<String, String> accountRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        Client client = authenticationService.getAuthenticatedClient(userDetails);
        // extract account details from the request body
        String accountName = accountRequest.get("accountName");
        String accountType = accountRequest.get("accountType");

        // check if the account type is provided
        Account createdAccount = accountService.createAndAttachAccount(client.getClientId(), accountName, accountType);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount); // returns the created account with status
    }

    // endpoint to delete a specific account
    @DeleteMapping("/{accountId}/delete")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable String accountId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String cardNumber = authenticationService.validateAuthenticatedUser(userDetails); // validate the authenticated user
        Client client = authenticationService.getClientByUserCardNumber(cardNumber); // Fetch the Client object

        // delete the account
        accountService.deleteAccount(accountId, client.getClientId());

        // return a no content response indicating successful deletion, no actual return
        return ResponseEntity.noContent().build();
    }
}
