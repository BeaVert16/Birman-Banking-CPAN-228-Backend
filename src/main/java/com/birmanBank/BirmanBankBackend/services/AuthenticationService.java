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
        // Fetch the user from the User table
        User appUser = userRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with card number: " + cardNumber));

        // Build the list of authorities
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add the primary role (e.g., "ADMIN", "CLIENT")
        if (appUser.getRole() != null && !appUser.getRole().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().toUpperCase()));
        }

        // If the user is a client, fetch the Client details and add activation-based
        // authorities
        if ("CLIENT".equalsIgnoreCase(appUser.getRole())) {
            Client client = clientRepository.findByUserCardNumber(appUser.getCardNumber())
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Client details not found for card number: " + cardNumber));

            // Add authority based on activation status
            if (client.getActivated() != null && client.getActivated()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ACTIVATED"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_DEACTIVATED"));
            }
        }

        // Return the UserDetails object with the constructed authorities
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

    public String validateAndExtractUsername(String authorizationHeader) {
        // Consider adding logging here for debugging token issues
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            // Returning null might be better than throwing here, let the filter handle it
            // throw new IllegalArgumentException("Invalid Authorization header");
            return null;
        }
        String token = authorizationHeader.substring(7);
        String username = null;
        try {
            // Extract username first
            username = jwtUtil.extractUsername(token);
            // Then validate (validation might check expiry, signature etc.)
            if (!jwtUtil.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
        } catch (Exception e) {
            // Log the exception
            System.err.println("Token validation/extraction failed: " + e.getMessage());
            // It's often better to return null and let the filter deny access
            // than to throw an exception that might expose internal details.
            return null;
            // throw new RuntimeException("Invalid or expired token", e);
        }
        return username; // Return extracted username if valid
    }

    public String getAuthenticatedCardNumber(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userDetails.getUsername();
    }

    // Modified generateToken to potentially use UserDetails for claims if needed
    public String generateToken(String username) {
        // Pass UserDetails or just username depending on your JwtUtil implementation
        return jwtUtil.generateToken(username); // Or jwtUtil.generateToken(username);
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

    public String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10));
        }
        cardNumber.append("8008");
        return cardNumber.toString();
    }
}