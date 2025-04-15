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

    // gets a client by its ID
    public Optional<Client> getClientById(String clientId) {
        return clientRepository.findById(clientId);
    }

    // updates a client by saving the client object - an overloaded version of updateClient
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
    // https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
    // https://www.baeldung.com/transaction-configuration-with-jpa-and-spring
    // transactional so that if any of the operations fail the entire transaction is rolled back
    @Transactional 
    public User registerClientAndUser(Client clientRequest, String password) {

        // registers a new client and associated user with the provided details and password.
        if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be null or empty");
        }

        String cardNumber = generateCardNumber();
        String encodedPassword = passwordEncoder.encode(password);

        // make sure the new card number is unique
        while (userRepository.findByCardNumber(cardNumber).isPresent()) {
            cardNumber = generateCardNumber();
        }

        // builds and saves a new user object with client role
        User user = User.builder()
                .cardNumber(cardNumber)
                .password(encodedPassword)
                .role("CLIENT") // Default role to CLIENT
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        // builds and saves a new client using information from the request abnd the generated card number
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

        // creates a new chequing account for the client
        Account account = Account.builder()
                .accountId(generateAccountId())
                .clientId(client.getClientId())
                .accountType("Chequing") // default account type
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        accountService.createAccount(account);

        return user;
    }

    // generates a random 12 digits number and appends "8008" to it to make it a proper 16 digits card number
    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10));
        }
        cardNumber.append("8008"); // apends "8008" to the end of the card number
        return cardNumber.toString();
    }

    // generates a unique account ID using current time in milliseconds
    private String generateAccountId() {
        return String.valueOf(System.currentTimeMillis());
    }

    // delete a client by its ID
    public void deleteClient(String clientId) {
        clientRepository.deleteById(clientId);
    }
}