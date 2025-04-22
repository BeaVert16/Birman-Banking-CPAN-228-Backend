package com.birmanBank.BirmanBankBackend.services;

import com.birmanBank.BirmanBankBackend.models.Loan;
import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Transaction;
import com.birmanBank.BirmanBankBackend.repositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.LoanRepository;
import com.birmanBank.BirmanBankBackend.repositories.TransactionRepository;
import com.birmanBank.BirmanBankBackend.utils.ValidationUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // process a user's request for loan.
    public Loan requestLoan(String userCard, BigDecimal amount) {
        ValidationUtil.validateCardNumber(userCard);
        ValidationUtil.validatePositiveAmount(amount, "Loan amount");

        Loan loan = Loan.builder()
                .userCardNumber(userCard)
                .amountRequested(amount)
                .amountOutstanding(amount)
                .status("PENDING")
                .loanDate(LocalDateTime.now())
                .build();
        return loanRepository.save(loan);
    }

    // get loans by user.
    public List<Loan> getLoansForUser(String userCard) {
        ValidationUtil.validateCardNumber(userCard);
        return loanRepository.findByUserCardNumber(userCard);
    }

    // get all pending loans.
    public List<Loan> getPendingLoans() {
        return loanRepository.findByStatus("PENDING");
    }

    // process for approving a loan.
    public Loan approveLoan(String loanId, String adminCard) {
        ValidationUtil.validateCardNumber(adminCard);
        ValidationUtil.validateNotEmpty(loanId, "Loan ID");

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));

        // get user's default balance account.
        Account acct = accountRepository.findByClientId(loan.getUserCardNumber())
                .stream()
                .filter(a -> "Chequing".equalsIgnoreCase(a.getAccountType()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No Chequing account for user " + loan.getUserCardNumber()));

        // credit balance.
        acct.setBalance(acct.getBalance().add(loan.getAmountRequested()));
        accountRepository.save(acct);

        // save transaction.
        Transaction transaction = Transaction.builder()
                .accountId(acct.getAccountId())
                .transactionType("CREDIT")
                .transactionAmount(loan.getAmountRequested())
                .postTransactionBalance(acct.getBalance())
                .timestamp(LocalDateTime.now())
                .description("Loan approved and disbursed")
                .build();
        transactionRepository.save(transaction);

        // update loan status.
        loan.setStatus("AWAITING_PAYMENT");
        loan.setDecidedAt(LocalDateTime.now());
        loan.setDecisionByAdmin(adminCard);
        return loanRepository.save(loan);
    }

    // deny loan.
    public void denyLoan(String loanId, String adminCard) {
        ValidationUtil.validateCardNumber(adminCard);
        ValidationUtil.validateNotEmpty(loanId, "Loan ID");

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        loan.setStatus("DENIED");
        loan.setDecidedAt(LocalDateTime.now());
        loan.setDecisionByAdmin(adminCard);
        loanRepository.save(loan);
    }

    // loan payment complete.
    public Loan payDownLoan(String loanId, BigDecimal payment) {
        ValidationUtil.validateNotEmpty(loanId, "Loan ID");
        ValidationUtil.validatePositiveAmount(payment, "Payment amount");

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));

        // get user's default balance account.
        Account acct = accountRepository.findByClientId(loan.getUserCardNumber())
                .stream()
                .filter(a -> "Chequing".equalsIgnoreCase(a.getAccountType()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No Chequing account for user " + loan.getUserCardNumber()));

        // sufficient balance check.
        ValidationUtil.validateSufficientBalance(acct.getBalance(), payment);

        // debit balance.
        acct.setBalance(acct.getBalance().subtract(payment));
        accountRepository.save(acct);

        // save transaction.
        Transaction transaction = Transaction.builder()
                .accountId(acct.getAccountId())
                .transactionType("DEBIT")
                .transactionAmount(payment)
                .postTransactionBalance(acct.getBalance())
                .timestamp(LocalDateTime.now())
                .description("Loan repayment")
                .build();
        transactionRepository.save(transaction);

        // update loan.
        BigDecimal remaining = loan.getAmountOutstanding().subtract(payment);
        loan.setAmountOutstanding(remaining.max(BigDecimal.ZERO));
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus("PAID");
        }
        return loanRepository.save(loan);
    }
}