package com.birmanBank.BirmanBankBackend.controllers.ClientControllers;

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

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthenticationService authenticationService;

    public TransactionController(TransactionService transactionService, AuthenticationService authenticationService) {
        this.transactionService = transactionService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, String>> transferMoney(
            @RequestBody Map<String, Object> transferRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Extract authenticated client ID from the token
        String authenticatedClientId = authenticationService.validateAuthenticatedUser(userDetails);

        // Extract data from the request
        String senderAccountId = (String) transferRequest.get("senderAccountId");
        String recipientPhoneNumber = (String) transferRequest.get("recipientPhoneNumber");

        BigDecimal amount;
        try {
            amount = new BigDecimal(transferRequest.get("amount").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid amount format"));
        }

        try {
            // Use authenticated client ID directly
            transactionService.transferMoney(authenticatedClientId, senderAccountId, recipientPhoneNumber, amount);
            return ResponseEntity.ok(Map.of("message", "Transfer successful"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, String>> depositMoney(
            @RequestBody Map<String, Object> depositRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Extract authenticated client ID from the token
        String authenticatedClientId = authenticationService.validateAuthenticatedUser(userDetails);

        // Extract data from the request
        String accountId = (String) depositRequest.get("accountId");

        BigDecimal amount;
        try {
            amount = new BigDecimal(depositRequest.get("amount").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid amount format"));
        }

        try {
            // Call the service to handle the deposit
            transactionService.depositMoney(authenticatedClientId, accountId, amount);
            return ResponseEntity.ok(Map.of("message", "Deposit successful"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }
}
