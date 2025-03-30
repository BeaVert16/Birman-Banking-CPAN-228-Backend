package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.springframework.stereotype.Service;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.repositories.UserRepositories.AccountRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    //-----------------------Constructors----------------------//
    private AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    //---------------------------------------------------------------//

    //create a new account
    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    //retrieve an account by its ID
    public Optional<Account> getAccountById(String accountId) {
        return accountRepository.findById(accountId);
    }

    //retrieve all accounts for a specific client (one-to-many relationship)
    public List<Account> getAccountsByClientId(String clientId) {
        return accountRepository.findByClientId(clientId);
    }

    //retrieve all accounts
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    // //update an account
    // public Account updateAccount(Account account) {
    //     return accountRepository.save(account);
    // }

    // //delete an account by its ID
    // public void deleteAccount(String accountId) {
    //     accountRepository.deleteById(accountId);
    // }
}
