package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.models.Address;
import com.birmanBank.BirmanBankBackend.repositories.UserRepository;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.ClientRepository;
import com.birmanBank.BirmanBankBackend.services.ClientServices.AccountService;
import com.birmanBank.BirmanBankBackend.utils.JwtUtil;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
            UserRepository userRepository, ClientRepository clientRepository,
            PasswordEncoder passwordEncoder, AccountService accountService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
    }

    // login endpoint
    // used to authenticate the user and generate a JWT token
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> loginRequest) {
        String cardNumber = loginRequest.get("cardNumber");
        String password = loginRequest.get("password");

        try {
            // authenticate the user using the card number and password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cardNumber, password));

            // generate a JWT token
            String token = jwtUtil.generateToken(authentication.getName());

            // return a response with the token
            Map<String, String> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("token", token);
            return response;

            // catch any authentication issues
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid card number or password");
        }
    }

    // register endpoint
    // used to register a new client
    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, Object> requestBody) {

        // extract the client details from the request body
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

        // extract the password from the request body to be used for the User password
        // (in User model)
        String password = (String) requestBody.get("password");
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        // grenerate a random 16-digit card number ending in 8008
        String cardNumber = generateCardNumber();

        // encode the password using BCrypt
        String encodedPassword = passwordEncoder.encode(password);

        // creates a new User object with the password from the request body
        User user = User.builder()
                .cardNumber(cardNumber)
                .password(encodedPassword)
                .role("CLIENT") // automatically assign the role as CLIENT
                .createdAt(LocalDateTime.now()) // log time of creation for admin
                .updatedAt(LocalDateTime.now()) // log update time for admin and logging purposes
                .build();

        // saves the user to the database
        userRepository.save(user);

        // creates a new Client object
        Client client = Client.builder()
                .clientId(generateClientId()) // generate a unique client ID
                .userCardNumber(cardNumber) // link the Users table's cardNumber to the Clients table so its like a
                                            // foreign key
                .firstName(clientRequest.getFirstName())
                .lastName(clientRequest.getLastName())
                .phoneNumber(clientRequest.getPhoneNumber())
                .email(clientRequest.getEmail())
                .address(clientRequest.getAddress())
                .sin(clientRequest.getSin())
                .createdAt(LocalDateTime.now())
                .build();

        // saves the client to the database
        clientRepository.save(client);

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

        // returns a response success message
        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("cardNumber", cardNumber); // return the generated card number so user can use it to login
        return response;
    }

    // helper method to generate a random 16-digit card number ending in 8008
    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();

        // generate the first 12 random digits
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10)); // Append a random digit (0-9)
        }

        // append "8008" to the end
        cardNumber.append("8008");

        return cardNumber.toString();
    }

    @GetMapping("/session-check")
    public Map<String, Object> sessionCheck(@RequestHeader("Authorization") String authorizationHeader) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Extract the token from the Authorization header
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Invalid Authorization header");
            }
            String token = authorizationHeader.substring(7);

            // Validate the token
            String username = jwtUtil.extractUsername(token);
            if (username == null || !jwtUtil.validateToken(token)) {
                throw new RuntimeException("Invalid or expired token");
            }

            // Retrieve the user details
            User user = userRepository.findByCardNumber(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Build the response
            response.put("isAuthenticated", true);
            response.put("user", Map.of(
                    "cardNumber", user.getCardNumber(),
                    "role", user.getRole(),
                    "isAdmin", "ADMIN".equals(user.getRole())));
        } catch (Exception e) {
            response.put("isAuthenticated", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    // helper method to generate a unique client ID
    private String generateClientId() {
        return "CLIENT-" + System.currentTimeMillis();
    }

    // Helper method to generate a unique account ID
    private String generateAccountId() {
        return "ACCOUNT-" + System.currentTimeMillis();
    }

}