package com.birmanBank.BirmanBankBackend.services;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;
import com.birmanBank.BirmanBankBackend.services.ClientServices.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository,
            ClientRepository clientRepository,
            AccountService accountService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerClient(Client clientRequest, String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be null or empty");
        }

        // Generate a unique card number
        String cardNumber = generateCardNumber();
        while (userRepository.findByCardNumber(cardNumber).isPresent()) {
            cardNumber = generateCardNumber();
        }
        String encodedPassword = passwordEncoder.encode(password);

        // Create User with the generated card number and encoded password
        User user = User.builder()
                .cardNumber(cardNumber)
                .password(encodedPassword)
                .role("CLIENT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        // Create the Client and set the client ID to the card number
        Client client = Client.builder()
                .clientId(cardNumber)
                .userCardNumber(cardNumber)
                .firstName(clientRequest.getFirstName())
                .lastName(clientRequest.getLastName())
                .phoneNumber(clientRequest.getPhoneNumber())
                .email(clientRequest.getEmail())
                .address(clientRequest.getAddress())
                .sin(clientRequest.getSin())
                .dateOfBirth(clientRequest.getDateOfBirth())
                .createdAt(LocalDateTime.now())
                .build();
        clientRepository.save(client);

        // Create a default Account (e.g., Chequing) for the client
        Account account = Account.builder()
                .accountId(generateAccountId())
                .clientId(client.getClientId())
                .accountType("Chequing")
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        accountService.createAccount(account);

        return user;
    }

    // Helper method to generate a unique card number
    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10));
        }
        // Append a constant suffix (consider making this configurable)
        cardNumber.append("8008");
        return cardNumber.toString();
    }

    // Helper method to generate an account ID
    private String generateAccountId() {
        // In a production system, consider using a more robust generation strategy
        // (e.g., UUID)
        return String.valueOf(System.currentTimeMillis());
    }
}
