package com.birmanBank.BirmanBankBackend.repositories;

import com.birmanBank.BirmanBankBackend.models.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends MongoRepository<Account, String> {
    Account findByCardNumber(int cardNumber); // find account using card number
}