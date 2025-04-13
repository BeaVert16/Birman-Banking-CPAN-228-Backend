package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;

import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.ClientRepository;

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

    // //update an account
    // public Account updateAccount(Account account) {
    // return accountRepository.save(account);
    // }

    // //delete an account by its ID
    // public void deleteAccount(String accountId) {
    // accountRepository.deleteById(accountId);
    // }
}