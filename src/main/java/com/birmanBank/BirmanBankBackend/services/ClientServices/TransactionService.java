package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Transaction;
import com.birmanBank.BirmanBankBackend.models.User;

import com.birmanBank.BirmanBankBackend.services.MessageService;

import com.birmanBank.BirmanBankBackend.repositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.TransactionRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;

import com.birmanBank.BirmanBankBackend.models.Client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public TransactionService(TransactionRepository transactionRepository,
            AccountRepository accountRepository, ClientRepository clientRepository,
            UserRepository userRepository, MessageService messageService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    // ------------------- Helper Methods -------------------

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction amount must be greater than zero");
        }
    }

    private Account findAccountById(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found: {}", accountId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountId);
                });
    }

    private Account validateAccountOwnership(String accountId, String expectedClientId, String accountRole) {
        Account account = findAccountById(accountId);
        if (!account.getClientId().equals(expectedClientId)) {
            logger.error("{} account {} does not belong to the authenticated user {}", accountRole, accountId,
                    expectedClientId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    accountRole + " account does not belong to the authenticated user");
        }
        return account;
    }

    private void checkSufficientBalance(Account account, BigDecimal amountNeeded) {
        if (account.getBalance().compareTo(amountNeeded) < 0) {
            logger.error("Insufficient balance in account {}: required={}, available={}",
                    account.getAccountId(), amountNeeded, account.getBalance());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient balance in account " + account.getAccountId());
        }
    }

    private void saveTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
        logger.info("Transaction record created: type={}, accountId={}, amount={}",
                transaction.getTransactionType(), transaction.getAccountId(), transaction.getTransactionAmount());
    }

    // ------------------- Public Service Methods -------------------

    // retrieve a transaction by its ID
    public Optional<Transaction> getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    // retrieve all transactions for a specific account with pagination
    public Page<Transaction> getTransactionsByAccountId(String accountId, Pageable pageable) {
        // Optional: Add check to ensure the requesting user owns the accountId
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    // retrieve transactions for a specific account within a date range with
    // pagination
    public Page<Transaction> getTransactionsByAccountIdAndDateRange(String accountId, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range provided.");
        }
        // Optional: Add check to ensure the requesting user owns the accountId
        return transactionRepository.findByAccountIdAndTimestampBetween(accountId, startDate, endDate, pageable);
    }

    // retrieve all transactions (Consider if this is needed/secure for clients)
    public List<Transaction> getAllTransactions() {
        // Be cautious exposing this directly to clients without authorization checks
        return transactionRepository.findAll();
    }

    @Transactional
    public void internalTransfer(String clientId, String fromAccountId, String toAccountId, BigDecimal amount) {
        logger.info("Initiating internal transfer: clientId={}, fromAccountId={}, toAccountId={}, amount={}",
                clientId, fromAccountId, toAccountId, amount);

        validateAmount(amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Source and destination accounts cannot be the same");
        }

        Account fromAccount = validateAccountOwnership(fromAccountId, clientId, "Source");
        Account toAccount = validateAccountOwnership(toAccountId, clientId, "Destination");

        // Calculate fee
        BigDecimal fee = BigDecimal.ZERO;
        if ("Savings".equalsIgnoreCase(fromAccount.getAccountType())) {
            fee = amount.multiply(new BigDecimal("0.015")); // 1.5% fee
            logger.info("Applying 1.5% fee ({}) for transfer from Savings account {}", fee, fromAccountId);
        }
        BigDecimal totalDeduction = amount.add(fee);

        checkSufficientBalance(fromAccount, totalDeduction);

        // Perform transfer
        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDeduction));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        logger.info("Internal transfer successful: fromAccountId={}, toAccountId={}, amount={}, fee={}",
                fromAccountId, toAccountId, amount, fee);

        // Create transaction records
        LocalDateTime now = LocalDateTime.now();
        String feeDescription = fee.compareTo(BigDecimal.ZERO) > 0 ? " (Fee: " + fee + ")" : "";

        Transaction debit = Transaction.builder()
                .accountId(fromAccount.getAccountId())
                .transactionType("DEBIT")
                .transactionAmount(totalDeduction)
                .postTransactionBalance(fromAccount.getBalance())
                .timestamp(now)
                .transferToAccountId(toAccount.getAccountId())
                .description("Internal transfer to account " + toAccount.getAccountId() + feeDescription)
                .build();

        Transaction credit = Transaction.builder()
                .accountId(toAccount.getAccountId())
                .transactionType("CREDIT")
                .transactionAmount(amount)
                .postTransactionBalance(toAccount.getBalance())
                .timestamp(now)
                .description("Internal transfer from account " + fromAccount.getAccountId())
                .build();

        saveTransaction(debit);
        saveTransaction(credit);
    }

    @Transactional
    public void transferMoney(String senderClientId, String senderAccountId, String recipientPhoneNumber,
            BigDecimal amount) {
        logger.info("Initiating transfer: senderClientId={}, senderAccountId={}, recipientPhoneNumber={}, amount={}",
                senderClientId, senderAccountId, recipientPhoneNumber, amount);

        validateAmount(amount);

        Account senderAccount = validateAccountOwnership(senderAccountId, senderClientId, "Sender");
        checkSufficientBalance(senderAccount, amount);

        // Find recipient client and account
        Client recipientClient = clientRepository.findByPhoneNumber(recipientPhoneNumber)
                .orElseThrow(() -> {
                    logger.error("Recipient not found with phone number: {}", recipientPhoneNumber);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found");
                });

        // Check if the recipient is an admin
        User recipientUser = userRepository.findByCardNumber(recipientClient.getClientId())
                .orElseThrow(() -> {
                    logger.error("Recipient user not found for clientId: {}", recipientClient.getClientId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found");
                });

        if ("ADMIN".equalsIgnoreCase(recipientUser.getRole())) {
            logger.error("Transfer to admin accounts is not allowed: recipientPhoneNumber={}", recipientPhoneNumber);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transfers to admin accounts are not allowed");
        }

        // Assuming recipient has at least one account, find the first one.
        Account recipientAccount = accountRepository.findByClientId(recipientClient.getClientId())
                .stream().findFirst()
                .orElseThrow(() -> {
                    logger.error("Recipient account not found for clientId: {}", recipientClient.getClientId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient account not found");
                });

        // Perform transfer
        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        recipientAccount.setBalance(recipientAccount.getBalance().add(amount));

        accountRepository.save(senderAccount);
        accountRepository.save(recipientAccount);

        logger.info("Transfer successful: senderAccountId={}, recipientAccountId={}, amount={}",
                senderAccountId, recipientAccount.getAccountId(), amount);

        // Create transaction records
        LocalDateTime now = LocalDateTime.now();
        Transaction debit = Transaction.builder()
                .accountId(senderAccount.getAccountId())
                .transactionType("DEBIT")
                .transactionAmount(amount)
                .postTransactionBalance(senderAccount.getBalance())
                .timestamp(now)
                .transferToAccountId(recipientAccount.getAccountId())
                .recipientPhoneNumber(recipientPhoneNumber)
                .description("Transfer to " + recipientPhoneNumber)
                .build();

        Transaction credit = Transaction.builder()
                .accountId(recipientAccount.getAccountId())
                .transactionType("CREDIT")
                .transactionAmount(amount)
                .postTransactionBalance(recipientAccount.getBalance())
                .timestamp(now)
                .description("Transfer received from client " + senderClientId)
                .build();

        saveTransaction(debit);
        saveTransaction(credit);

        // Send a notification to the recipient's inbox
        String subject = "Money Received";
        String body = "You have received a transfer of $" + amount + " from client ID: " + senderClientId;
        messageService.sendMessage(recipientClient.getClientId(), subject, body);
    }

    @Transactional // Ensure atomicity
    public void depositMoney(String clientId, String accountId, BigDecimal amount) {
        logger.info("Initiating deposit: clientId={}, accountId={}, amount={}", clientId, accountId, amount);

        validateAmount(amount);
        Account account = validateAccountOwnership(accountId, clientId, "Deposit");

        // Perform deposit
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        logger.info("Deposit successful: accountId={}, amount={}", accountId, amount);

        // Create transaction record
        Transaction deposit = Transaction.builder()
                .accountId(account.getAccountId())
                .transactionType("CREDIT")
                .transactionAmount(amount)
                .postTransactionBalance(account.getBalance())
                .timestamp(LocalDateTime.now())
                .description("Deposit")
                .build();

        saveTransaction(deposit);
    }
}