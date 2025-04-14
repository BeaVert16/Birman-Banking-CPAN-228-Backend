package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.InboxMessage;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String cardNumber = loginRequest.get("cardNumber");
        String password = loginRequest.get("password");
        Map<String, Object> response = new HashMap<>();

        if (cardNumber == null || cardNumber.trim().isEmpty() || password == null || password.isEmpty()) {
            response.put("message", "Card number and password are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cardNumber, password));

            String token = authenticationService.generateToken(authentication.getName());
            User user = userRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            response.put("message", "Login successful");
            response.put("token", token);
            response.put("role", user.getRole()); // Include the user's role in the response
            System.out.println("User found: " + response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("message", "Invalid credentials or user not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, Object> requestBody) {
        String password = (String) requestBody.get("password");
        if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be null or empty");
        }

        Client clientRequest = new Client();
        clientRequest.setFirstName((String) requestBody.get("firstName"));
        clientRequest.setLastName((String) requestBody.get("lastName"));
        clientRequest.setPhoneNumber((String) requestBody.get("phoneNumber"));
        clientRequest.setEmail((String) requestBody.get("email"));

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
                .Activated(false)
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

        // Inside the register method
        Account account = accountService.createAndAttachAccount(
                client.getClientId(),
                "",
                "Chequing");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("cardNumber", cardNumber);

        List<User> admins = userRepository.findAll().stream()
                .filter(adminUser -> "ADMIN".equalsIgnoreCase(adminUser.getRole()))
                .toList();

        for (User admin : admins) {
            messageService.sendMessage(
                    admin.getCardNumber(),
                    "New User Registration",
                    "A new user with card number " + cardNumber + " has registered and is awaiting activation.");
        }

        return response;
    }

    @GetMapping("/session-check")
    public ResponseEntity<?> sessionCheck(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Validate and extract the username (card number) from the token
            String authorizationHeader = request.getHeader("Authorization");
            String cardNumber = authenticationService.validateAndExtractUsername(authorizationHeader);

            // Fetch the user from the User table
            User user = userRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Prepare the response for admin users
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                response.put("isAuthenticated", true);
                response.put("user", Map.of(
                        "cardNumber", user.getCardNumber(),
                        "role", user.getRole(),
                        "isAdmin", true));
            } else {
                // Fetch the client details for non-admin users
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

        System.out.println("Session check response: " + response);
        return ResponseEntity.ok(response);
    }

    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10));
        }
        cardNumber.append("8008");
        return cardNumber.toString();
    }
}