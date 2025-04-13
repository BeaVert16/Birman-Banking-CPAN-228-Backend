package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;

import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.ClientRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    // -----------------------Constructors----------------------//
    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;

    public AccountService(AccountRepository accountRepository, ClientRepository clientRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }
    // ---------------------------------------------------------------//

    // create a new account
    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    // retrieve an account by its ID
    public Optional<Account> getAccountById(String accountId) {
        return accountRepository.findById(accountId);
    }

    // retrieve all accounts for a specific client (one-to-many relationship)
    public List<Account> getAccountsByClientId(String clientId) {
        return accountRepository.findByClientId(clientId);
    }

    // retrieve all accounts
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    // retrieve all accounts for a user by their card number (moved from controller)
    public List<Account> getAccountsForAuthenticatedUser(String cardNumber) {
        Client client = clientRepository.findByUserCardNumber(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Client associated with token not found"));
        return accountRepository.findByClientId(client.getClientId());
    }

    public Account createAndAttachAccount(String clientId, String accountName, String accountType) {
        // Validate account type
        if (!accountType.equalsIgnoreCase("Chequing") && !accountType.equalsIgnoreCase("Savings")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid account type. Must be 'Chequing' or 'Savings'.");
        }

        // Generate a unique account ID
        String accountId = generateAccountId();

        // Create the account object
        Account account = Account.builder()
                .accountId(accountId)
                .clientId(clientId)
                .accountName(accountName)
                .accountType(accountType)
                .balance(BigDecimal.ZERO) // Default balance
                .status("ACTIVE") // Default status
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Save the account to the database
        return accountRepository.save(account);
    }

    public Account updateAccountName(String accountId, String clientId, String newAccountName) {
        // Retrieve the account by ID
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));

        // Ensure the account belongs to the client
        if (!account.getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to update this account.");
        }

        // Update the account name
        account.setAccountName(newAccountName);
        account.setUpdatedAt(LocalDateTime.now());

        // Save the updated account
        return accountRepository.save(account);
    }

    // Helper method to generate a unique account ID
    private String generateAccountId() {
        return String.valueOf(System.currentTimeMillis());
    }

    // //update an account
    // public Account updateAccount(Account account) {
    // return accountRepository.save(account);
    // }

    // //delete an account by its ID
    // public void deleteAccount(String accountId) {
    // accountRepository.deleteById(accountId);
    // }
}