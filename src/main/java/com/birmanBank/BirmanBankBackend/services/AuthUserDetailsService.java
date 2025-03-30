package com.birmanBank.BirmanBankBackend.services;

import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AuthUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String cardNumber) throws UsernameNotFoundException {
        //fetch the user from the database using the card number
        User appUser = userRepository.findById(cardNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with card number: " + cardNumber));

        //create a UserDetails object
        //converts the User entity to something that Spring Security can understand
        return org.springframework.security.core.userdetails.User.withUsername(appUser.getCardNumber())
                .password(appUser.getPassword())
                .roles(appUser.getRole())
                .build();
    }
}
