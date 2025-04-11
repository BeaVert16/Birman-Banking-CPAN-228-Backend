package com.birmanBank.BirmanBankBackend.repositories.ClientRepositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.birmanBank.BirmanBankBackend.models.Transaction;

import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    //retrieve all transactions for a specific account (one-to-many relationship)
    //will return a list of transactions associated with the given account ID
    List<Transaction> findByAccountId(String accountId);
}
