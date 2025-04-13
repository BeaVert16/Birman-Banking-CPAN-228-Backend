package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Transaction;
import com.birmanBank.BirmanBankBackend.models.Client;

import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.TransactionRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.ClientRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    // -----------------------Constructors----------------------//
    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;
    private ClientRepository clientRepository;

    public TransactionService(TransactionRepository transactionRepository,
            AccountRepository accountRepository, ClientRepository clientRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }
    // ---------------------------------------------------------------//

    // create a new transaction
    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    // retrieve a transaction by its ID
    public Optional<Transaction> getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    // retrieve all transactions for a specific account (one-to-many relationship)
    // with pagination
    public Page<Transaction> getTransactionsByAccountId(String accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    // retrieve transactions for a specific account within a date range with
    // pagination
    public Page<Transaction> getTransactionsByAccountIdAndDateRange(String accountId, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range provided.");
        }
        return transactionRepository.findByAccountIdAndTimestampBetween(accountId, startDate, endDate, pageable);
    }

    // retrieve all transactions
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public void transferMoney(String senderClientId, String senderAccountId, String recipientPhoneNumber,
            BigDecimal amount) {
        logger.info("Initiating transfer: senderClientId={}, senderAccountId={}, recipientPhoneNumber={}, amount={}",
                senderClientId, senderAccountId, recipientPhoneNumber, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount must be greater than zero");
        }

        // Validate sender's account ownership
        Account senderAccount = accountRepository.findById(senderAccountId)
                .orElseThrow(() -> {
                    logger.error("Sender account not found: {}", senderAccountId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender account not found");
                });

        if (!senderAccount.getClientId().equals(senderClientId)) {
            logger.error("Sender account does not belong to the authenticated user: {}", senderClientId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Sender account does not belong to the authenticated user");
        }

        if (senderAccount.getBalance().compareTo(amount) < 0) {
            logger.error("Insufficient balance in sender's account: {}", senderAccountId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        // Find recipient's account using phone number
        Client recipientClient = clientRepository.findByPhoneNumber(recipientPhoneNumber)
                .orElseThrow(() -> {
                    logger.error("Recipient not found with phone number: {}", recipientPhoneNumber);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found");
                });

        Account recipientAccount = accountRepository.findByClientId(recipientClient.getClientId())
                .stream().findFirst() // Assuming the recipient has one account
                .orElseThrow(() -> {
                    logger.error("Recipient account not found for clientId: {}", recipientClient.getClientId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient account not found");
                });

        // Perform the transfer
        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        recipientAccount.setBalance(recipientAccount.getBalance().add(amount));

        accountRepository.save(senderAccount);
        accountRepository.save(recipientAccount);

        logger.info("Transfer successful: senderAccountId={}, recipientAccountId={}, amount={}",
                senderAccountId, recipientAccount.getAccountId(), amount);

        // Create transaction records
        Transaction senderTransaction = Transaction.builder()
                .accountId(senderAccount.getAccountId())
                .transactionType("DEBIT")
                .transactionAmount(amount)
                .postTransactionBalance(senderAccount.getBalance())
                .timestamp(LocalDateTime.now())
                .transferToAccountId(recipientAccount.getAccountId())
                .description("Transfer to " + recipientPhoneNumber)
                .build();

        Transaction recipientTransaction = Transaction.builder()
                .accountId(recipientAccount.getAccountId())
                .transactionType("CREDIT")
                .transactionAmount(amount)
                .postTransactionBalance(recipientAccount.getBalance())
                .timestamp(LocalDateTime.now())
                .description("Transfer from " + senderClientId)
                .build();

        transactionRepository.save(senderTransaction);
        transactionRepository.save(recipientTransaction);

        logger.info("Transaction records created successfully for transfer");
    }

    public void depositMoney(String clientId, String accountId, BigDecimal amount) {
        logger.info("Initiating deposit: clientId={}, accountId={}, amount={}", clientId, accountId, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deposit amount must be greater than zero");
        }

        // Validate account ownership
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found: {}", accountId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
                });

        if (!account.getClientId().equals(clientId)) {
            logger.error("Account does not belong to the authenticated user: {}", clientId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Account does not belong to the authenticated user");
        }

        // Perform the deposit
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        logger.info("Deposit successful: accountId={}, amount={}", accountId, amount);

        // Create a transaction record
        Transaction depositTransaction = Transaction.builder()
                .accountId(account.getAccountId())
                .transactionType("CREDIT")
                .transactionAmount(amount)
                .postTransactionBalance(account.getBalance())
                .timestamp(LocalDateTime.now())
                .description("Deposit")
                .build();

        transactionRepository.save(depositTransaction);

        logger.info("Transaction record created successfully for deposit");
    }

    // //update a transaction
    // public Transaction updateTransaction(Transaction transaction) {
    // return transactionRepository.save(transaction);
    // }

    // //delete a transaction by its ID
    // public void deleteTransaction(String transactionId) {
    // transactionRepository.deleteById(transactionId);
    // }
}