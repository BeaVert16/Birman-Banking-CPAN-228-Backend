package com.birmanBank.BirmanBankBackend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.birmanBank.BirmanBankBackend.models.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    //see if a user with the card number already exists
    //will return a boolean value indicating whether the user exists
    Optional<User> findByCardNumber(String cardNumber);
}