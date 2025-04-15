package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.services.AuthenticationService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * TransactionController handles the transaction-related endpoints.
 * It provides methods for transferring money, depositing money, and performing internal transfers.
 */

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    //-----------------------Constructors----------------------//
    private final TransactionService transactionService;
    private final AuthenticationService authenticationService;

    public TransactionController(TransactionService transactionService, AuthenticationService authenticationService) {
        this.transactionService = transactionService;
        this.authenticationService = authenticationService;
    }
    // ---------------------------------------------------------------//

    // ndepoint to transfer money between accounts
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, String>> transferMoney(
            @RequestBody Map<String, Object> transferRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // get the authenticated client ID from the token and validate the user
        String authenticatedClientId = authenticationService.validateAuthenticatedUser(userDetails);
        String senderAccountId = (String) transferRequest.get("senderAccountId"); // get the sender account ID from the request
        String recipientPhoneNumber = (String) transferRequest.get("recipientPhoneNumber"); // get the recipient phone number from the request


        BigDecimal amount;
        try {
            // parse the amount from the request and convert it to BigDecimal
            amount = new BigDecimal(transferRequest.get("amount").toString());
        } catch (NumberFormatException e) {
            // handle the case where the amount is not a valid number
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid amount format"));
        }

        try {
            // start the transfer process by calling the service
            transactionService.transferMoney(authenticatedClientId, senderAccountId, recipientPhoneNumber, amount);
            return ResponseEntity.ok(Map.of("message", "Transfer successful")); //if it completes successfully
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    // endpoint to deposit money into an account
    @PostMapping("/deposit")
    public ResponseEntity<Map<String, String>> depositMoney(
            @RequestBody Map<String, Object> depositRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        // get the authenticated client ID from the token and validate the user
        String authenticatedClientId = authenticationService.validateAuthenticatedUser(userDetails);
        String accountId = (String) depositRequest.get("accountId"); // get the account ID from the request

        BigDecimal amount;
        try {
            // parse the amount from the request and convert it to BigDecimal
            amount = new BigDecimal(depositRequest.get("amount").toString());
        } catch (NumberFormatException e) {
            // handle the case where the amount is not a valid number
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid amount format"));
        }

        try {
            // start the transfer process by calling the service
            transactionService.depositMoney(authenticatedClientId, accountId, amount);
            return ResponseEntity.ok(Map.of("message", "Deposit successful")); //if it completes successfully
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @PostMapping("/internal-transfer")
    public ResponseEntity<Map<String, String>> internalTransfer(
            @RequestBody Map<String, Object> transferRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        // get the authenticated client ID from the token and validate the user
        String authenticatedClientId = authenticationService.validateAuthenticatedUser(userDetails);

        // 
        String fromAccountId = (String) transferRequest.get("fromAccountId"); // get the sender account ID from the request
        String toAccountId = (String) transferRequest.get("toAccountId"); // get the recipient account ID from the request

        BigDecimal amount;
        try {
             // parse the amount from the request and convert it to BigDecimal
            amount = new BigDecimal(transferRequest.get("amount").toString());
        } catch (NumberFormatException | NullPointerException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or missing amount format"));
        }

        if (fromAccountId == null || toAccountId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Missing fromAccountId or toAccountId"));
        }

        try {
            // start the internal transfer process by calling the service
            transactionService.internalTransfer(authenticatedClientId, fromAccountId, toAccountId, amount);
            return ResponseEntity.ok(Map.of("message", "Internal transfer successful"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred during the transfer"));
        }
    }
}
