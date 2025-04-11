package com.birmanBank.BirmanBankBackend.repositories.ClientRepositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.birmanBank.BirmanBankBackend.models.Client;

import java.util.Optional;

public interface ClientRepository extends MongoRepository<Client, String> {
    //find a client by their user card number
    //will return an Optional<Client> object, which may or may not contain a Client object
    Optional<Client> findByUserCardNumber(String userCardNumber);
}

