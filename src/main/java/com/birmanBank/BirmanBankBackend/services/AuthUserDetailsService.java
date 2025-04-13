package com.birmanBank.BirmanBankBackend.services;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;

    public AuthUserDetailsService(UserRepository userRepository, AccountRepository accountRepository,
            ClientRepository clientRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String cardNumber) throws UsernameNotFoundException {
        User appUser = userRepository.findById(cardNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with card number: " + cardNumber));

        return org.springframework.security.core.userdetails.User.withUsername(appUser.getCardNumber())
                .password(appUser.getPassword())
                .roles(appUser.getRole())
                .build();
    }

    public void verifyAccountOwnership(String accountId, String cardNumber) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Client client = clientRepository.findById(account.getClientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Client associated with account not found"));

        if (!client.getUserCardNumber().equals(cardNumber)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access to account");
        }
    }
}