package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Loan;
import com.birmanBank.BirmanBankBackend.services.LoanService;
import com.birmanBank.BirmanBankBackend.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/loans")
@RequiredArgsConstructor
public class AdminLoanController {
    private final LoanService loanService;
    private final AuthenticationService authService;

    // endpoint to get all loans pending approval.
    @GetMapping("/pending")
    public ResponseEntity<List<Loan>> getPendingLoans(@RequestHeader("Authorization") String auth) {
        String admin = authService.validateAndExtractUsername(auth);
        return ResponseEntity.ok(loanService.getPendingLoans());
    }

    // endpoint to approve a loan.
    @PostMapping("/{loanId}/approve")
    public ResponseEntity<Loan> approve(@PathVariable String loanId,
                                        @RequestHeader("Authorization") String auth) {
        String admin = authService.validateAndExtractUsername(auth);
        return ResponseEntity.ok(loanService.approveLoan(loanId, admin));
    }

    // endpoint to deny a loan.
    @PostMapping("/{loanId}/deny")
    public ResponseEntity<Loan> deny(@PathVariable String loanId,
                                     @RequestHeader("Authorization") String auth) {
        String admin = authService.validateAndExtractUsername(auth);
        loanService.denyLoan(loanId, admin);
        return ResponseEntity.noContent().build();
    }
}