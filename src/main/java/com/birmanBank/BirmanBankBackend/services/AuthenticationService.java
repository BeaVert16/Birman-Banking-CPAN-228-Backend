package com.birmanBank.BirmanBankBackend.services;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;
import com.birmanBank.BirmanBankBackend.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticationService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final JwtUtil jwtUtil;

    public AuthenticationService(UserRepository userRepository, AccountRepository accountRepository,
            ClientRepository clientRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
        this.jwtUtil = jwtUtil;
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

    public String validateAndExtractUsername(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        String token = authorizationHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        return jwtUtil.extractUsername(token);
    }

    public String getAuthenticatedCardNumber(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userDetails.getUsername();
    }

    public String generateToken(String username) {
        return jwtUtil.generateToken(username);
    }

    public String validateAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userDetails.getUsername();
    }

    public Client getAuthenticatedClient(String cardNumber) {
        return clientRepository.findByUserCardNumber(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    public void verifyPhoneNumberOwnership(String phoneNumber, String cardNumber) {
        Client client = clientRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phone number not found"));
    
        if (!client.getUserCardNumber().equals(cardNumber)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access to this phone number");
        }
    }
}