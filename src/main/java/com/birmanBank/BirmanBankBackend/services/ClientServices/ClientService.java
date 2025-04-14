package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository; 

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class ClientService {

    // -----------------------Constructors----------------------//
    private ClientRepository clientRepository;
    private UserRepository userRepository;
    private AccountService accountService;
    private PasswordEncoder passwordEncoder;

    public ClientService(ClientRepository clientRepository,
            UserRepository userRepository, AccountService accountService, PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
    }
    // ---------------------------------------------------------------//

    // create a new client
    public Client createClient(Client client) {
        return clientRepository.save(client);
    }

    // retrieve a client by its ID
    public Optional<Client> getClientById(String clientId) {
        return clientRepository.findById(clientId);
    }

    // retrieve a client by its user card number
    public Optional<Client> getClientByUserCardNumber(String userCardNumber) {
        return clientRepository.findByUserCardNumber(userCardNumber);
    }

    // retrieve a client by its phone number
    public Optional<Client> getClientByPhoneNumber(String phoneNumber) {
        return clientRepository.findByPhoneNumber(phoneNumber);
    }

    // retrieve all clients
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    // New Registration Method
    @Transactional // Ensure atomicity
    public User registerClientAndUser(Client clientRequest, String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be null or empty");
        }
        // Consider adding more validation for clientRequest fields here

        String cardNumber = generateCardNumber();
        String encodedPassword = passwordEncoder.encode(password);

        // Ensure generated card number is unique
        while (userRepository.findByCardNumber(cardNumber).isPresent()) {
            cardNumber = generateCardNumber();
        }

        User user = User.builder()
                .cardNumber(cardNumber)
                .password(encodedPassword)
                .role("CLIENT") // Default role to CLIENT
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        Client client = Client.builder()
                .clientId(cardNumber) // Use card number as client ID
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

        Account account = Account.builder()
                .accountId(generateAccountId())
                .clientId(client.getClientId())
                .accountType("Chequing") // Default account type
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        accountService.createAccount(account);

        return user; // Return the created user (contains the card number)
    }

    // Helper method moved from AuthController
    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10));
        }
        cardNumber.append("8008"); // Consider making this suffix configurable or more robust
        return cardNumber.toString();
    }

    // Helper method moved from AuthController
    private String generateAccountId() {
        // Consider a more robust unique ID generation strategy (e.g., UUID)
        return String.valueOf(System.currentTimeMillis());
    }

    //update a client
    public Client updateClient(Client client) {
    return clientRepository.save(client);
    }

    // delete a client by its ID
    public void deleteClient(String clientId) {
    clientRepository.deleteById(clientId);
    }
}