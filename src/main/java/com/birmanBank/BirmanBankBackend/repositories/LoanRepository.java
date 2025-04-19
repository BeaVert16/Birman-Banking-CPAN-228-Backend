package com.birmanBank.BirmanBankBackend.repositories;

import com.birmanBank.BirmanBankBackend.models.Loan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LoanRepository extends MongoRepository<Loan, String> {
    List<Loan> findByUserCardNumber(String cardNumber); // find loan by user.
    List<Loan> findByStatus(String status); // find loan by status.
}
