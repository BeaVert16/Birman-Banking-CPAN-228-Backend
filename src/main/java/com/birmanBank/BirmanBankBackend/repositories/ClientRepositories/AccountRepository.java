package com.birmanBank.BirmanBankBackend.repositories.ClientRepositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.birmanBank.BirmanBankBackend.models.Account;

import java.util.List;

public interface AccountRepository extends MongoRepository<Account, String> {
    //retrieve all accounts for a specific client (one-to-many relationship)
    //will return a list of accounts associated with the given client ID
    List<Account> findByClientId(String clientId);
}