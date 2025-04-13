package com.birmanBank.BirmanBankBackend.services;

import org.springframework.stereotype.Service;

import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    //-----------------------Constructors----------------------//
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    //---------------------------------------------------------------//

    //create a new user
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    //retrieve a user by card number
    public Optional<User> getUserByCardNumber(String cardNumber) {
        return userRepository.findById(cardNumber);
    }
    
    //retrieve all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // //update a user
    // public User updateUser(User user) {
    //     return userRepository.save(user);
    // }
    
    // //delete a user by card number
    // public void deleteUser(String cardNumber) {
    //     userRepository.deleteById(cardNumber);
    // }
}