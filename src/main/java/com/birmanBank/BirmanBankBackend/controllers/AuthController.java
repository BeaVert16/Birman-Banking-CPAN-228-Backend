package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.models.Address;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;

import com.birmanBank.BirmanBankBackend.services.AuthenticationService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.AccountService;
import com.birmanBank.BirmanBankBackend.services.MessageService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AuthController handles authentication and registration requests.
 * It provides endpoints for user login, registration, and session validation.
 */

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    //-----------------------Constructors----------------------//
    private final AuthenticationManager authenticationManager;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    private final MessageService messageService;

    public AuthController(AuthenticationManager authenticationManager, AuthenticationService authenticationService,
            UserRepository userRepository, ClientRepository clientRepository,
            PasswordEncoder passwordEncoder, AccountService accountService,
            MessageService messageService) {
        this.authenticationManager = authenticationManager;
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
        this.messageService = messageService;
    }
    // ---------------------------------------------------------------//

    // endpoint to handle user login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String cardNumber = loginRequest.get("cardNumber"); // get the card number from the request
        String password = loginRequest.get("password"); // get the password from the request
        Map<String, Object> response = new HashMap<>(); // create a response map

        // validate the input
        if (cardNumber == null || cardNumber.trim().isEmpty() || password == null || password.isEmpty()) {
            response.put("message", "Card number and password are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            // authenticate the user using the provided credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cardNumber, password));

            // generate a JWT token for the authenticated user
            String token = authenticationService.generateToken(authentication.getName());

            // fetch the user from the database using the card number
            User user = userRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // debugging stuff
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("role", user.getRole());
            System.out.println("User found: " + response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("message", "Invalid credentials or user not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // endpoint to handle user registration
    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, Object> requestBody) {
        String password = (String) requestBody.get("password"); // get the password from the request body

        Client clientRequest = new Client(); // create a new client object
        clientRequest.setFirstName((String) requestBody.get("firstName"));
        clientRequest.setLastName((String) requestBody.get("lastName"));
        clientRequest.setPhoneNumber((String) requestBody.get("phoneNumber"));
        clientRequest.setEmail((String) requestBody.get("email"));
        
        authenticationService.isPhoneNumberUnique(clientRequest.getPhoneNumber());

        // extract client details from the request body and builds the address object
        Address address = Address.builder()
                .address((String) requestBody.get("address"))
                .city((String) requestBody.get("city"))
                .postalCode((String) requestBody.get("postalCode"))
                .additionalInfo((String) requestBody.get("additionalInfo"))
                .province((String) requestBody.get("province"))
                .country((String) requestBody.get("country"))
                .build();
        clientRequest.setAddress(address);

        clientRequest.setSin((String) requestBody.get("sin"));
        clientRequest.setDateOfBirth((String) requestBody.get("dateOfBirth"));

        String cardNumber = authenticationService.generateCardNumber(); // generate a unique card number
        String encodedPassword = passwordEncoder.encode(password); // encode the password

        // check if the card number already exists in the database
        // if it does it generates a new one
        while (userRepository.findByCardNumber(cardNumber).isPresent()) {
            cardNumber = authenticationService.generateCardNumber();
        }

        // create a new user and client object and save them to the database
        User user = User.builder()
                .cardNumber(cardNumber)
                .password(encodedPassword)
                .role("CLIENT") // Default role to CLIENT
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        // builds and saves the client object
        Client client = Client.builder()
                .clientId(cardNumber)
                .userCardNumber(cardNumber)
                .Activated(true)
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

        // create an account for the new client
        Account account = accountService.createAndAttachAccount(
                client.getClientId(),
                "",
                "Chequing");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("cardNumber", cardNumber); // cardNumber is the same as client.getClientId() here

        // List<User> admins = userRepository.findAll().stream()
        //         .filter(adminUser -> "ADMIN".equalsIgnoreCase(adminUser.getRole()))
        //         .toList();

        // for (User admin : admins) {
        //     messageService.sendRegistrationMessage( // Changed from sendMessage
        //             admin.getCardNumber(),
        //             "New User Registration",
        //             "A new user with card number " + client.getClientId()
        //                     + " has registered and is awaiting activation.",
        //             client.getClientId()
        //     );
        // }

        return response;
    }

    // endpoint to check if the user is authenticated
    @GetMapping("/session-check")
    public ResponseEntity<?> sessionCheck(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // validate and extract the username (card number) from the token
            String authorizationHeader = request.getHeader("Authorization");
            String cardNumber = authenticationService.validateAndExtractUsername(authorizationHeader);

            // fetch the user from the User table
            User user = userRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // check if the user is an admin
            // if the user is an admin it defines the response for the admin
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                response.put("isAuthenticated", true);
                response.put("user", Map.of(
                        "cardNumber", user.getCardNumber(),
                        "role", user.getRole(),
                        "isAdmin", true));
            } else {
                // fetch the client from the Client table
                // if the user is not an admin it defines the response for the client
                Client client = clientRepository.findByUserCardNumber(user.getCardNumber())
                        .orElseThrow(() -> new RuntimeException("Client not found"));

                response.put("isAuthenticated", true);
                response.put("user", Map.of(
                        "cardNumber", user.getCardNumber(),
                        "role", user.getRole(),
                        "isAdmin", false,
                        "firstName", client.getFirstName(),
                        "activated", client.getActivated()));
            }
        } catch (Exception e) {
            response.put("isAuthenticated", false);
            response.put("error", e.getMessage());
        }

        System.out.println("Session check response: " + response); //debugging stuff
        return ResponseEntity.ok(response);
    }

    // private String generateCardNumber() {
    //     Random random = new Random();
    //     StringBuilder cardNumber = new StringBuilder();
    //     for (int i = 0; i < 12; i++) {
    //         cardNumber.append(random.nextInt(10));
    //     }
    //     cardNumber.append("8008");
    //     return cardNumber.toString();
    // }
}