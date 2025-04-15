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

    // -----------------------Constructors----------------------//
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
    // ---------------------------------------------------------------//

    // validate the transaction amount (greater than zero)
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction amount must be greater than zero");
        }
    }

    // find an account by its ID and check if it belongs to the authenticated user
    private Account findAccountById(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found: {}", accountId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountId);
                });
    }

    // validate that the account belongs to the authenticated user
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

    // check if the account has sufficient balance for the transaction
    private void checkSufficientBalance(Account account, BigDecimal amountNeeded) {
        if (account.getBalance().compareTo(amountNeeded) < 0) {
            logger.error("Insufficient balance in account {}: required={}, available={}",
                    account.getAccountId(), amountNeeded, account.getBalance());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient balance in account " + account.getAccountId());
        }
    }

    // save a transaction record
    private void saveTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
        logger.info("Transaction record created: type={}, accountId={}, amount={}",
                transaction.getTransactionType(), transaction.getAccountId(), transaction.getTransactionAmount());
    }

    // get a transaction by its ID
    public Optional<Transaction> getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    // get all transactions for a specific account with pagination
    public Page<Transaction> getTransactionsByAccountId(String accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    // get all transactions for a specific client with pagination
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // handles internal transfers between accounts owned by the same client
    @Transactional
    public void internalTransfer(String clientId, String fromAccountId, String toAccountId, BigDecimal amount) {
        logger.info("Initiating internal transfer: clientId={}, fromAccountId={}, toAccountId={}, amount={}",
                clientId, fromAccountId, toAccountId, amount);

        validateAmount(amount);

        // check if the source and destination accounts are different
        if (fromAccountId.equals(toAccountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Source and destination accounts cannot be the same");
        }

        // check if the source and destination accounts belong to the same client
        Account fromAccount = validateAccountOwnership(fromAccountId, clientId, "Source");
        Account toAccount = validateAccountOwnership(toAccountId, clientId, "Destination");

        // calculate the transfer fee if the source is a savings account
        BigDecimal fee = BigDecimal.ZERO;
        if ("Savings".equalsIgnoreCase(fromAccount.getAccountType())) {
            fee = amount.multiply(new BigDecimal("0.015")); // 1.5% fee
            logger.info("Applying 1.5% fee ({}) for transfer from Savings account {}", fee, fromAccountId);
        }
        BigDecimal totalDeduction = amount.add(fee);

        // check if the source account has sufficient balance
        checkSufficientBalance(fromAccount, totalDeduction);


        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDeduction)); // deduct the amount and fee from source account
        toAccount.setBalance(toAccount.getBalance().add(amount)); // add the amount to destination account

        //save the updated accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        logger.info("Internal transfer successful: fromAccountId={}, toAccountId={}, amount={}, fee={}",
                fromAccountId, toAccountId, amount, fee);

        // creates the transaction records
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

    // handles transfers between different clients
    @Transactional
    public void transferMoney(String senderClientId, String senderAccountId, String recipientPhoneNumber,
            BigDecimal amount) {

        // logs the transfer initiation
        logger.info("Initiating transfer: senderClientId={}, senderAccountId={}, recipientPhoneNumber={}, amount={}",
                senderClientId, senderAccountId, recipientPhoneNumber, amount);

        validateAmount(amount); // check if the amount is valid

        // check the ownership of the sender's account and check balance
        Account senderAccount = validateAccountOwnership(senderAccountId, senderClientId, "Sender");
        checkSufficientBalance(senderAccount, amount);

        //find the recipient client by phone number
        Client recipientClient = clientRepository.findByPhoneNumber(recipientPhoneNumber)
                .orElseThrow(() -> {
                    logger.error("Recipient not found with phone number: {}", recipientPhoneNumber);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found");
                });

        //find the recipient user by client ID
        User recipientUser = userRepository.findByCardNumber(recipientClient.getClientId())
                .orElseThrow(() -> {
                    logger.error("Recipient user not found for clientId: {}", recipientClient.getClientId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found");
                });

        // prevents transfers to admin accounts
        if ("ADMIN".equalsIgnoreCase(recipientUser.getRole())) {
            logger.error("Transfer to admin accounts is not allowed: recipientPhoneNumber={}", recipientPhoneNumber);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transfers to admin accounts are not allowed");
        }

        //find the recipient account by client ID
        Account recipientAccount = accountRepository.findByClientId(recipientClient.getClientId())
                .stream().findFirst()
                .orElseThrow(() -> {
                    logger.error("Recipient account not found for clientId: {}", recipientClient.getClientId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient account not found");
                });

        // deduct the amount from the sender's account and add it to the recipient's account
        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        recipientAccount.setBalance(recipientAccount.getBalance().add(amount));

        accountRepository.save(senderAccount);
        accountRepository.save(recipientAccount);

        logger.info("Transfer successful: senderAccountId={}, recipientAccountId={}, amount={}",
                senderAccountId, recipientAccount.getAccountId(), amount);

        // creates transaction record
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

        // send notification to recipient
        String subject = "Money Received";
        String body = "You have received a transfer of $" + amount + " from client ID: " + senderClientId;
        messageService.sendMessage(recipientClient.getClientId(), subject, body);
    }

    // handles deposits into an account
    @Transactional
    public void depositMoney(String clientId, String accountId, BigDecimal amount) {
        logger.info("Initiating deposit: clientId={}, accountId={}, amount={}", clientId, accountId, amount);

        validateAmount(amount);
        Account account = validateAccountOwnership(accountId, clientId, "Deposit");

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        logger.info("Deposit successful: accountId={}, amount={}", accountId, amount);

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