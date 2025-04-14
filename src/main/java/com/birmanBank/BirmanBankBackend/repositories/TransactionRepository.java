package com.birmanBank.BirmanBankBackend.repositories;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.birmanBank.BirmanBankBackend.models.Transaction;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    // retrieve all transactions for a specific account (one-to-many relationship)
    // will return a list of transactions associated with the given account ID
    Page<Transaction> findByAccountId(String accountId, Pageable pageable);

    Page<Transaction> findByAccountIdAndTimestampBetween(String accountId, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable);

}