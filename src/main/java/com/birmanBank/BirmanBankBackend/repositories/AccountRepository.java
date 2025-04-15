package com.birmanBank.BirmanBankBackend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.birmanBank.BirmanBankBackend.models.Account;

import java.util.List;

public interface AccountRepository extends MongoRepository<Account, String> {
    // retrieve all accounts for a specific client (one-to-many relationship)
    // will return a list of accounts associated with the given client ID
    List<Account> findByClientId(String clientId);

    // custom query to find an account by the client's phone number
    @Query("{ 'client.phoneNumber': ?0 }")
    List<Account> findByClientPhoneNumber(String phoneNumber);
}