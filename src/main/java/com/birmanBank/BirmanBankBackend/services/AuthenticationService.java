package com.birmanBank.BirmanBankBackend.services;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.repositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;
import com.birmanBank.BirmanBankBackend.utils.JwtUtil;
import com.birmanBank.BirmanBankBackend.utils.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    public boolean isPhoneNumberUnique(String phoneNumber) {
        ValidationUtil.validatePhoneNumber(phoneNumber);
        ValidationUtil.validateUniquePhoneNumber(phoneNumber, clientRepository);
        return true;
    }

    public User getClientByCardNumber(String cardNumber) {
        ValidationUtil.validateCardNumber(cardNumber);
        return userRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Client getClientByUserCardNumber(String cardNumber) {
        ValidationUtil.validateCardNumber(cardNumber);
        return clientRepository.findByUserCardNumber(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    public boolean isAdmin(User user) {
        return "ADMIN".equalsIgnoreCase(user.getRole());
    }

    @Override
    public UserDetails loadUserByUsername(String cardNumber) throws UsernameNotFoundException {
        ValidationUtil.validateCardNumber(cardNumber);
        User appUser = userRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with card number: " + cardNumber));

        List<GrantedAuthority> authorities = new ArrayList<>();

        if (appUser.getRole() != null && !appUser.getRole().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().toUpperCase()));
        }

        if ("CLIENT".equalsIgnoreCase(appUser.getRole())) {
            Client client = clientRepository.findByUserCardNumber(appUser.getCardNumber())
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Client details not found for card number: " + cardNumber));

            if (client.getActivated() != null && client.getActivated()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ACTIVATED"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_DEACTIVATED"));
            }
        }

        return new org.springframework.security.core.userdetails.User(
                appUser.getCardNumber(),
                appUser.getPassword(),
                authorities);
    }

    public void verifyAccountOwnership(String accountId, String cardNumber) {
        ValidationUtil.validateCardNumber(cardNumber);
        ValidationUtil.validateNotEmpty(accountId, "Account ID");
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Client client = clientRepository.findById(account.getClientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Client associated with account not found"));

        ValidationUtil.validateAccountOwnership(client.getUserCardNumber(), cardNumber);
    }

    public String validateAndExtractUsername(String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        ValidationUtil.validateToken(token, jwtUtil);

        try {
            return jwtUtil.extractUsername(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token validation failed: " + e.getMessage());
        }
    }

    public String getAuthenticatedCardNumber(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authenticated");
        }
        return userDetails.getUsername();
    }

    public String generateToken(String username) {
        return jwtUtil.generateToken(username);
    }

    public String validateAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authenticated");
        }
        return userDetails.getUsername();
    }

    public String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10));
        }
        cardNumber.append("8008");
        return cardNumber.toString();
    }

    public void validateUserAndAccountOwnership(UserDetails userDetails, String accountId) {
        String cardNumber = validateAuthenticatedUser(userDetails);
        verifyAccountOwnership(accountId, cardNumber);
    }

    public Client getAuthenticatedClient(UserDetails userDetails) {
        String cardNumber = validateAuthenticatedUser(userDetails);
        return getClientByUserCardNumber(cardNumber);
    }
}