package com.birmanBank.BirmanBankBackend.controllers.ClientControllers;

import com.birmanBank.BirmanBankBackend.controllers.AccountDetailsDto;
import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Transaction;
import com.birmanBank.BirmanBankBackend.services.AuthUserDetailsService;
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

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuthUserDetailsService authUserDetailsService;

    public AccountController(AccountService accountService, TransactionService transactionService,
            AuthUserDetailsService authUserDetailsService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.authUserDetailsService = authUserDetailsService;
    }

    @GetMapping
    public ResponseEntity<List<Account>> getUserAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String cardNumber = userDetails.getUsername();
        List<Account> accounts = accountService.getAccountsForAuthenticatedUser(cardNumber);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}/basic")
    public ResponseEntity<Account> getAccountBasicInfo(@PathVariable String accountId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String cardNumber = userDetails.getUsername();
        authUserDetailsService.verifyAccountOwnership(accountId, cardNumber);

        Account account = accountService.getAccountById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountId}/details")
    public ResponseEntity<AccountDetailsDto> getAccountDetails(
            @PathVariable String accountId,
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {

        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String cardNumber = userDetails.getUsername();
        authUserDetailsService.verifyAccountOwnership(accountId, cardNumber);

        Account account = accountService.getAccountById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Page<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId, pageable);

        AccountDetailsDto responseDto = new AccountDetailsDto(account, transactions);
        return ResponseEntity.ok(responseDto);
    }
}