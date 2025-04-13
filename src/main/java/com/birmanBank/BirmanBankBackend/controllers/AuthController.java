package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.models.Address;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.services.ClientServices.AccountService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.ClientService;
import com.birmanBank.BirmanBankBackend.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    private final ClientService clientService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
            UserRepository userRepository, ClientRepository clientRepository,
            PasswordEncoder passwordEncoder, AccountService accountService,
            ClientService clientService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
        this.clientService = clientService;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> loginRequest) {
        String cardNumber = loginRequest.get("cardNumber");
        String password = loginRequest.get("password");

        if (cardNumber == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card number and password are required");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cardNumber, password));

            String token = jwtUtil.generateToken(authentication.getName());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("token", token);
            return response;

        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
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

        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("cardNumber", cardNumber);
        return response;
    }

    @GetMapping("/session-check")
    public ResponseEntity<?> sessionCheck(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Invalid Authorization header");
            }
            String token = authorizationHeader.substring(7);

            String username = jwtUtil.extractUsername(token);
            if (username == null || !jwtUtil.validateToken(token)) {
                throw new RuntimeException("Invalid or expired token");
            }

            User user = userRepository.findByCardNumber(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Client client = clientRepository.findByUserCardNumber(user.getCardNumber())
                    .orElseThrow(() -> new RuntimeException("Client not found"));

            response.put("isAuthenticated", true);
            response.put("user", Map.of(
                    "cardNumber", user.getCardNumber(),
                    "role", user.getRole(),
                    "isAdmin", "ADMIN".equals(user.getRole()),
                    "firstName", client.getFirstName()));

        } catch (Exception e) {
            response.put("isAuthenticated", false);
            response.put("error", e.getMessage());
        }

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

    private String generateAccountId() {
        return String.valueOf(System.currentTimeMillis());
    }
}