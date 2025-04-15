package com.birmanBank.BirmanBankBackend.services;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.repositories.AccountRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;
import com.birmanBank.BirmanBankBackend.utils.JwtUtil;

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

    // -----------------------Constructors----------------------//
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
    // ---------------------------------------------------------------//

    // checks if the phone number is unique
    public boolean isPhoneNumberUnique(String phoneNumber) {

        boolean exists = clientRepository.findByPhoneNumber(phoneNumber).isPresent();
        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number already in use");
        }
        return true;
    }

    // loads a user by there card num for spring security
    @Override
    public UserDetails loadUserByUsername(String cardNumber) throws UsernameNotFoundException {
        // get the user by card number
        User appUser = userRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with card number: " + cardNumber));

        // build the list of authorities
        List<GrantedAuthority> authorities = new ArrayList<>();

        // add users main role to the list of authorities
        if (appUser.getRole() != null && !appUser.getRole().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().toUpperCase()));
        }

        // if the user is a client, fetch the Client details and add activation-based
        // authorities
        if ("CLIENT".equalsIgnoreCase(appUser.getRole())) {
            Client client = clientRepository.findByUserCardNumber(appUser.getCardNumber())
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Client details not found for card number: " + cardNumber));

            // add authority based on activation status
            if (client.getActivated() != null && client.getActivated()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ACTIVATED"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_DEACTIVATED"));
            }
        }

        return new org.springframework.security.core.userdetails.User(
                appUser.getCardNumber(), // Principal identifier
                appUser.getPassword(), // Encoded password
                authorities // List of granted authorities
        );
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

    // extracts username (card number) from jwt token in authorization header
    public String validateAndExtractUsername(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null; // return null if header is missing or malformed
        }

        String token = authorizationHeader.substring(7); // remove "Bearer " prefix
        try {
            String username = jwtUtil.extractUsername(token); // extract username from token

            // validate token (checks signature, expiration, etc.)
            if (!jwtUtil.validateToken(token)) {
                return null; // return null if token is invalid
            }

            return username; // return valid extracted username
        } catch (Exception e) {
            System.err.println("token validation/extraction failed: " + e.getMessage());
            return null; // return null if extraction or validation fails
        }
    }

    // returns authenticated user's card number from userdetails
    public String getAuthenticatedCardNumber(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authenticated");
        }
        return userDetails.getUsername(); // spring security uses username for card number
    }

    // generates a jwt token using the user's card number
    public String generateToken(String username) {
        return jwtUtil.generateToken(username); // delegate to jwt util
    }

    // validates that the user is authenticated and returns their card number
    public String validateAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authenticated");
        }
        return userDetails.getUsername();
    }

    // fetches the client associated with the given card number
    public Client getAuthenticatedClient(String cardNumber) {
        return clientRepository.findByUserCardNumber(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "client not found"));
    }

    // generates a random card number with 12 digits + "8008" suffix
    public String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10)); // append a random digit
        }
        cardNumber.append("8008"); // custom suffix
        return cardNumber.toString();
    }
}