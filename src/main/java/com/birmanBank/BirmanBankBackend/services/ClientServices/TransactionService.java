package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.birmanBank.BirmanBankBackend.models.Transaction;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.TransactionRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    //-----------------------Constructors----------------------//
    private TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    //---------------------------------------------------------------//

    //create a new transaction
    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    //retrieve a transaction by its ID
    public Optional<Transaction> getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    //retrieve all transactions for a specific account (one-to-many relationship) with pagination
    public Page<Transaction> getTransactionsByAccountId(String accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    //retrieve all transactions
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // //update a transaction
    // public Transaction updateTransaction(Transaction transaction) {
    //     return transactionRepository.save(transaction);
    // }

    // //delete a transaction by its ID
    // public void deleteTransaction(String transactionId) {
    //     transactionRepository.deleteById(transactionId);
    // }
}