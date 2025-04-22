package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;
import com.birmanBank.BirmanBankBackend.services.AuthenticationService;

import com.birmanBank.BirmanBankBackend.utils.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class ClientService {

    // -----------------------Constructors----------------------//
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

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

    // gets a client by its ID
    public Optional<Client> getClientById(String clientId) {
        return clientRepository.findById(clientId);
    }

    // updates a client by saving the client object - an overloaded version of
    // updateClient
    public Client updateClient(Client client) {
        return clientRepository.save(client);
    }

    // gets a client using their user card number
    public Optional<Client> getClientByUserCardNumber(String userCardNumber) {
        return clientRepository.findByUserCardNumber(userCardNumber);
    }

    // gets a client by its phone number
    public Optional<Client> getClientByPhoneNumber(String phoneNumber) {
        return clientRepository.findByPhoneNumber(phoneNumber);
    }

    // gets all list of clients
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    // New Registration Method
    @Transactional
    public User registerClientAndUser(Client clientRequest, String password) {
        // Validate password
        ValidationUtil.validateNotEmpty(password, "Password");

        // Validate phone number uniqueness
        ValidationUtil.validateUniquePhoneNumber(clientRequest.getPhoneNumber(), clientRepository);

        String cardNumber = generateCardNumber();
        String encodedPassword = passwordEncoder.encode(password);

        // Ensure the new card number is unique
        while (userRepository.findByCardNumber(cardNumber).isPresent()) {
            cardNumber = generateCardNumber();
        }

        // Build and save a new user object with client role
        User user = User.builder()
                .cardNumber(cardNumber)
                .password(encodedPassword)
                .role("CLIENT") // Default role to CLIENT
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        // Build and save a new client using information from the request and the
        // generated card number
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

        // Create a new chequing account for the client
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

        return user;
    }

    // gets the authenticated client using the user details and authentication
    // service
    public Client getAuthenticatedClient(UserDetails userDetails, AuthenticationService authenticationService) {
        // Validate the authenticated user and fetch the card number
        String cardNumber = authenticationService.validateAuthenticatedUser(userDetails);

        // Fetch the client details using the card number
        return getClientByUserCardNumber(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    // generates a random 12 digits number and appends "8008" to it to make it a
    // proper 16 digits card number
    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10));
        }
        cardNumber.append("8008"); // Append "8008" to the end of the card number
        return cardNumber.toString();
    }

    // generates a unique account ID using current time in milliseconds
    private String generateAccountId() {
        return String.valueOf(System.currentTimeMillis());
    }

    // deletes a client by its ID
    public void deleteClient(String clientId) {
        // validate if the client exists
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        clientRepository.deleteById(clientId);
    }
}