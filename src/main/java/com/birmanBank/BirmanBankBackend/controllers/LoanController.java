package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Loan;
import com.birmanBank.BirmanBankBackend.services.LoanService;
import com.birmanBank.BirmanBankBackend.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;
    private final AuthenticationService authService;

    // endpoint for user requesting a loan.
    @PostMapping("/request")
    public ResponseEntity<Loan> requestLoan(@RequestBody Map<String, String> body,
                                            @RequestHeader("Authorization") String auth) {
        String userCard = authService.validateAndExtractUsername(auth);
        BigDecimal amount = new BigDecimal(body.get("amount"));
        Loan loan = loanService.requestLoan(userCard, amount);
        return ResponseEntity.ok(loan);
    }

    // endpoint that lists all outstanding loans.
    @GetMapping
    public ResponseEntity<List<Loan>> getMyLoans(@RequestHeader("Authorization") String auth) {
        String userCard = authService.validateAndExtractUsername(auth);
        return ResponseEntity.ok(loanService.getLoansForUser(userCard));
    }

    // endpoint for making a loan payment.
    @PostMapping("/{loanId}/pay")
    public ResponseEntity<Loan> payLoan(@PathVariable String loanId,
                                        @RequestBody Map<String, String> body,
                                        @RequestHeader("Authorization") String auth) {
        authService.validateAndExtractUsername(auth);
        BigDecimal payment = new BigDecimal(body.get("amount"));
        return ResponseEntity.ok(loanService.payDownLoan(loanId, payment));
    }
}
