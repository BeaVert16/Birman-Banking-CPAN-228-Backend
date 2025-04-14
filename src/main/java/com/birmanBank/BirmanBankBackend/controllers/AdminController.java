package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Account;
import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.Transaction;
import com.birmanBank.BirmanBankBackend.models.User;
import com.birmanBank.BirmanBankBackend.models.InboxMessage;

import com.birmanBank.BirmanBankBackend.services.ClientServices.AccountService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.ClientService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.TransactionService;
import com.birmanBank.BirmanBankBackend.services.UserService;
import com.birmanBank.BirmanBankBackend.services.MessageService;

import com.birmanBank.BirmanBankBackend.repositories.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ClientService clientService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public AdminController(ClientService clientService, AccountService accountService,
            TransactionService transactionService, UserService userService, PasswordEncoder passwordEncoder,
            UserRepository userRepository, MessageService messageService) {
        this.clientService = clientService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    // ------------------- User Management Endpoints -------------------

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{cardNumber}")
    public ResponseEntity<User> getUserByCardNumber(@PathVariable String cardNumber) {
        Optional<User> user = userService.getUserByCardNumber(cardNumber);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/users/{cardNumber}")
    public ResponseEntity<User> updateUser(@PathVariable String cardNumber, @RequestBody User updatedUser) {
        Optional<User> existingUser = userService.getUserByCardNumber(cardNumber);
        if (existingUser.isPresent()) {
            updatedUser.setCardNumber(cardNumber);
            User savedUser = userService.createUser(updatedUser);
            return ResponseEntity.ok(savedUser);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @DeleteMapping("/users/{cardNumber}")
    public ResponseEntity<Void> deleteUser(@PathVariable String cardNumber) {
        userService.deleteUser(cardNumber);
        return ResponseEntity.noContent().build();
    }

    // ------------------- Client Management Endpoints -------------------

    @GetMapping("/clients")
    public ResponseEntity<List<Client>> getAllClients() {
        List<Client> clients = clientService.getAllClients();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/clients/{clientId}")
    public ResponseEntity<Client> getClientById(@PathVariable String clientId) {
        Optional<Client> client = clientService.getClientById(clientId);
        return client.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping("/clients")
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        Client createdClient = clientService.createClient(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    @PutMapping("/clients/{clientId}")
    public ResponseEntity<Client> updateClient(@PathVariable String clientId, @RequestBody Client updatedClient) {
        Optional<Client> existingClient = clientService.getClientById(clientId);
        if (existingClient.isPresent()) {
            updatedClient.setClientId(clientId);
            Client savedClient = clientService.createClient(updatedClient);
            return ResponseEntity.ok(savedClient);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @DeleteMapping("/clients/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable String clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    // ------------------- Account Management Endpoints -------------------

    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<Account> getAccountById(@PathVariable String accountId) {
        Optional<Account> account = accountService.getAccountById(accountId);
        return account.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        Account createdAccount = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @PutMapping("/accounts/{accountId}")
    public ResponseEntity<Account> updateAccount(@PathVariable String accountId, @RequestBody Account updatedAccount) {
        Optional<Account> existingAccount = accountService.getAccountById(accountId);
        if (existingAccount.isPresent()) {
            updatedAccount.setAccountId(accountId);
            Account savedAccount = accountService.createAccount(updatedAccount);
            return ResponseEntity.ok(savedAccount);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    // ------------------- Transaction Management Endpoints -------------------

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable String transactionId) {
        Optional<Transaction> transaction = transactionService.getTransactionById(transactionId);
        return transaction.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping("/create-admin")
    public ResponseEntity<User> createAdminAccount(@RequestBody Map<String, String> adminDetails) {
        String cardNumber = adminDetails.get("cardNumber");
        String password = adminDetails.get("password");

        String encodedPassword = passwordEncoder.encode(password);

        // Create a new admin user
        User adminUser = new User();
        adminUser.setCardNumber(cardNumber);
        adminUser.setPassword(encodedPassword);
        adminUser.setRole("ADMIN");
        adminUser.setCreatedAt(java.time.LocalDateTime.now());
        adminUser.setUpdatedAt(java.time.LocalDateTime.now());

        // Save the admin user to the database
        User savedAdmin = userRepository.save(adminUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedAdmin);
    }

    @PostMapping("/inbox/{messageId}/{action}")
    public ResponseEntity<String> processInboxMessageAction(
            @PathVariable String messageId, // Changed from Long to String
            @PathVariable String action) {
        Optional<InboxMessage> messageOptional = messageService.getMessageById(messageId);
    
        if (messageOptional.isEmpty()) {
            return ResponseEntity.status(404).body("Message not found");
        }
    
        InboxMessage message = messageOptional.get();
    
        if ("accept".equalsIgnoreCase(action)) {
            message.setStatus("ACCEPTED");
        } else if ("deny".equalsIgnoreCase(action)) {
            message.setStatus("DENIED");
        } else {
            return ResponseEntity.status(400).body("Invalid action");
        }
    
        message.setUpdatedAt(LocalDateTime.now());
        messageService.saveMessage(message);
    
        return ResponseEntity.ok("Message " + action + "ed successfully");
    }
}