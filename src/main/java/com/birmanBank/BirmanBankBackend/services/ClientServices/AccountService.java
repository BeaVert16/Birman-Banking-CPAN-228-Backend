package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.repositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepository;

import com.birmanBank.BirmanBankBackend.utils.ValidationUtil;

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
        ValidationUtil.validateCardNumber(cardNumber);
        Client client = clientRepository.findByUserCardNumber(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Client associated with token not found"));
        return accountRepository.findByClientId(client.getClientId());
    }

    public Account createAndAttachAccount(String clientId, String accountName, String accountType) {
        // validate account type
        ValidationUtil.validateAccountType(accountType);

        // generates a unique account ID
        String accountId = generateAccountId();

        // creates a new account object
        Account account = Account.builder()
                .accountId(accountId)
                .clientId(clientId)
                .accountName(accountName)
                .accountType(accountType)
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return accountRepository.save(account); // saves the account to the database
    }

    public Account updateAccountName(String accountId, String clientId, String newAccountName) {
        // get the account by ID
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));

        // validate account ownership
        ValidationUtil.validateAccountOwnership(account.getClientId(), clientId);

        // update the account name with new name
        account.setAccountName(newAccountName);

        // updates last modified timestamp
        account.setUpdatedAt(LocalDateTime.now());

        // Save the updated account
        return accountRepository.save(account);
    }

    // generates a unique account ID based on the current time in milliseconds
    private String generateAccountId() {
        return String.valueOf(System.currentTimeMillis());
    }

    public void deleteAccount(String accountId, String clientId) {
        // get the account by ID
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));

        // validate account ownership
        ValidationUtil.validateAccountOwnership(account.getClientId(), clientId);

        // get all accounts for the client
        List<Account> clientAccounts = accountRepository.findByClientId(clientId);

        // check if the client has only one account
        if (clientAccounts.size() == 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot delete your only account.");
        }

        // try to identify the original account created with the client's profile
        Account originalAccount = clientAccounts.stream()
                .min((a1, a2) -> a1.getCreatedAt().compareTo(a2.getCreatedAt()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unable to determine the original account."));

        if (originalAccount.getAccountId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot delete the original account created with your profile.");
        }

        // proceed to delete the account
        accountRepository.delete(account);
    }

    // delete an account by its ID
    public void deleteAccount(String accountId) {
        accountRepository.deleteById(accountId);
    }

    public Account getAccountOrThrow(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }
}