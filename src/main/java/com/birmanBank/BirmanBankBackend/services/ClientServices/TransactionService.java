package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Transaction;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.models.Client;

import com.birmanBank.BirmanBankBackend.repositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.TransactionRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;

import com.birmanBank.BirmanBankBackend.services.MessageService;
import com.birmanBank.BirmanBankBackend.utils.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    // -----------------------Constructors----------------------//
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public TransactionService(TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            ClientRepository clientRepository,
            UserRepository userRepository,
            MessageService messageService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }
    // ---------------------------------------------------------------//

    // find an account by its ID and check if it exists
    private Account findAccountById(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountId));
    }

    // save a transaction record
    private void saveTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
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
        // validate the transfer amount
        ValidationUtil.validatePositiveAmount(amount, "Transfer amount");

        // check if the source and destination accounts are the same
        if (fromAccountId.equals(toAccountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Source and destination accounts cannot be the same");
        }

        // find and validate ownership of the source account
        Account fromAccount = findAccountById(fromAccountId);
        ValidationUtil.validateAccountOwnership(fromAccount.getClientId(), clientId);

        // find and validate ownership of the destination account
        Account toAccount = findAccountById(toAccountId);
        ValidationUtil.validateAccountOwnership(toAccount.getClientId(), clientId);

        // calculate the transfer fee if the source account is a savings account
        BigDecimal fee = BigDecimal.ZERO;
        if ("Savings".equalsIgnoreCase(fromAccount.getAccountType())) {
            fee = amount.multiply(new BigDecimal("0.015")); // 1.5% fee
        }
        BigDecimal totalDeduction = amount.add(fee);

        // validate that the source account has sufficient balance
        ValidationUtil.validateSufficientBalance(fromAccount.getBalance(), totalDeduction);

        // update balances for both accounts
        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDeduction));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // save the updated accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // create transaction records for both accounts
        LocalDateTime now = LocalDateTime.now();
        String feeDescription = fee.compareTo(BigDecimal.ZERO) > 0 ? " (Fee: " + fee + ")" : "";

        saveTransaction(Transaction.builder()
                .accountId(fromAccount.getAccountId())
                .transactionType("DEBIT")
                .transactionAmount(totalDeduction)
                .postTransactionBalance(fromAccount.getBalance())
                .timestamp(now)
                .transferToAccountId(toAccount.getAccountId())
                .description("Internal transfer to account " + toAccount.getAccountId() + feeDescription)
                .build());

        saveTransaction(Transaction.builder()
                .accountId(toAccount.getAccountId())
                .transactionType("CREDIT")
                .transactionAmount(amount)
                .postTransactionBalance(toAccount.getBalance())
                .timestamp(now)
                .description("Internal transfer from account " + fromAccount.getAccountId())
                .build());
    }

    // handles transfers between different clients
    @Transactional
    public void transferMoney(String senderClientId, String senderAccountId, String recipientPhoneNumber,
            BigDecimal amount) {
        // validate the transfer amount
        ValidationUtil.validatePositiveAmount(amount, "Transfer amount");

        // find and validate ownership of the sender's account
        Account senderAccount = findAccountById(senderAccountId);
        ValidationUtil.validateAccountOwnership(senderAccount.getClientId(), senderClientId);

        // validate that the sender's account has sufficient balance
        ValidationUtil.validateSufficientBalance(senderAccount.getBalance(), amount);

        // find the recipient client by phone number
        Client recipientClient = clientRepository.findByPhoneNumber(recipientPhoneNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));

        // find the recipient user by client ID
        User recipientUser = userRepository.findByCardNumber(recipientClient.getClientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found"));

        // validate that the recipient is not an admin
        ValidationUtil.validateNonAdminTransfer(recipientUser.getRole());

        // find the recipient account by client ID
        Account recipientAccount = accountRepository.findByClientId(recipientClient.getClientId())
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient account not found"));

        // update balances for both accounts
        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        recipientAccount.setBalance(recipientAccount.getBalance().add(amount));

        // save the updated accounts
        accountRepository.save(senderAccount);
        accountRepository.save(recipientAccount);

        // create transaction records for both accounts
        LocalDateTime now = LocalDateTime.now();

        saveTransaction(Transaction.builder()
                .accountId(senderAccount.getAccountId())
                .transactionType("DEBIT")
                .transactionAmount(amount)
                .postTransactionBalance(senderAccount.getBalance())
                .timestamp(now)
                .transferToAccountId(recipientAccount.getAccountId())
                .recipientPhoneNumber(recipientPhoneNumber)
                .description("Transfer to " + recipientPhoneNumber)
                .build());

        saveTransaction(Transaction.builder()
                .accountId(recipientAccount.getAccountId())
                .transactionType("CREDIT")
                .transactionAmount(amount)
                .postTransactionBalance(recipientAccount.getBalance())
                .timestamp(now)
                .description("Transfer received from client " + senderClientId)
                .build());

        // send notification to the recipient
        String subject = "Money Received";
        String body = "You have received a transfer of $" + amount + " from client ID: " + senderClientId;
        messageService.sendMessage(recipientClient.getClientId(), subject, body);
    }

    // handles deposits into an account
    @Transactional
    public void depositMoney(String clientId, String accountId, BigDecimal amount) {
        // validate the deposit amount
        ValidationUtil.validatePositiveAmount(amount, "Deposit amount");

        // find and validate ownership of the account
        Account account = findAccountById(accountId);
        ValidationUtil.validateAccountOwnership(account.getClientId(), clientId);

        // update the account balance
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // create a transaction record for the deposit
        saveTransaction(Transaction.builder()
                .accountId(account.getAccountId())
                .transactionType("CREDIT")
                .transactionAmount(amount)
                .postTransactionBalance(account.getBalance())
                .timestamp(LocalDateTime.now())
                .description("Deposit")
                .build());
    }
}