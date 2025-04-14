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

/**
 * AdminController handles requests related to admin operations
 * provides endpoints for managing users, clients, accounts, and transactions
 * also includes an endpoint for creating admin accounts
 */

@RestController
@RequestMapping("/api/admin")
public class AdminController {


    //-----------------------Constructors----------------------//
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
    // ---------------------------------------------------------------//

    // endpoint to get all clients
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        // fetch all users from the userService and returns them
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // endpoint to get a user by card number
    @GetMapping("/users/{cardNumber}")
    public ResponseEntity<User> getUserByCardNumber(@PathVariable String cardNumber) {
        // fetch a user by card number from the userService
        Optional<User> user = userService.getUserByCardNumber(cardNumber);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // endpoint to create a new user
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {

        // calls the userService to create a new user
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    // endpoint to update an existing user
    @PutMapping("/users/{cardNumber}")
    public ResponseEntity<User> updateUser(@PathVariable String cardNumber, @RequestBody User updatedUser) {

        // fetch the existing user by card number
        // if the user exists, update the user details and save it else -return a 404 Not Found response
        Optional<User> existingUser = userService.getUserByCardNumber(cardNumber);
        if (existingUser.isPresent()) {
            updatedUser.setCardNumber(cardNumber);
            // update the user card number using the userService
            User savedUser = userService.createUser(updatedUser);
            return ResponseEntity.ok(savedUser);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    // endpoint to delete a user by card number
    @DeleteMapping("/users/{cardNumber}")
    public ResponseEntity<Void> deleteUser(@PathVariable String cardNumber) {
        // calls the userService to delete a user by card number
        userService.deleteUser(cardNumber);
        return ResponseEntity.noContent().build();
    }

    // endpoint to get all clients
    @GetMapping("/clients")
    public ResponseEntity<List<Client>> getAllClients() {
        // fetch all clients from the clientService and return them
        List<Client> clients = clientService.getAllClients();
        return ResponseEntity.ok(clients);
    }

    // endpoint to get a client by ID
    @GetMapping("/clients/{clientId}")
    public ResponseEntity<Client> getClientById(@PathVariable String clientId) {
        // fetch a client by ID from the clientService
        // if the client exists, return it else return a 404 Not Found response
        Optional<Client> client = clientService.getClientById(clientId);
        return client.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // endpoint to create a new client
    @PostMapping("/clients")
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        // calls the clientService to create a new client
        Client createdClient = clientService.createClient(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    // endpoint to update an existing client
    @PutMapping("/clients/{clientId}")
    public ResponseEntity<Client> updateClient(@PathVariable String clientId, @RequestBody Client updatedClient) {

        // fetch the existing client by ID
        // if the client exists, update the client details and save it else- return a 404 Not Found response
        Optional<Client> existingClient = clientService.getClientById(clientId);
        if (existingClient.isPresent()) {
            updatedClient.setClientId(clientId);
            // update the client id using the clientService
            Client savedClient = clientService.createClient(updatedClient);
            return ResponseEntity.ok(savedClient);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    // endpoint to delete a client by ID
    @DeleteMapping("/clients/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable String clientId) {
        // calls the clientService to delete a client by ID
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    // endpoint to get all accounts
    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAllAccounts() {
        // fetch all accounts from the accountSevice and return them
        List<Account> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    // endpoint to get an account by ID
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<Account> getAccountById(@PathVariable String accountId) {
        // fetch an account by ID from the accountService
        // if the account exists, return it else return a 404 Not Found response
        Optional<Account> account = accountService.getAccountById(accountId);
        return account.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // endpoint to create a new account
    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        // calls the accountService to create a new account
        Account createdAccount = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    // endpoint to update an existing account
    @PutMapping("/accounts/{accountId}")
    public ResponseEntity<Account> updateAccount(@PathVariable String accountId, @RequestBody Account updatedAccount) {

        // fetch the existing account by ID
        // if the account exists update the account details and save it else- return a 404 Not Found response
        Optional<Account> existingAccount = accountService.getAccountById(accountId);
        if (existingAccount.isPresent()) {
            updatedAccount.setAccountId(accountId);
            // update the account id using the accountService
            Account savedAccount = accountService.createAccount(updatedAccount);
            return ResponseEntity.ok(savedAccount);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    // endpoint to delete an account by ID
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    // endpoint to get all transactions
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        // fetch all transactions from the transactionService and return them
        List<Transaction> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    // endpoint to get a transaction by ID
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable String transactionId) {
        // fetch a transaction by ID from the transactionService
        // if the transaction exists, retrun it else return a 404 Not Found response
        Optional<Transaction> transaction = transactionService.getTransactionById(transactionId);
        return transaction.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // endpoint to make a new admin account
    @PostMapping("/create-admin")
    public ResponseEntity<User> createAdminAccount(@RequestBody Map<String, String> adminDetails) {

        // get the card number and password from the request body
        String cardNumber = adminDetails.get("cardNumber");
        String password = adminDetails.get("password");

        // check if the card number already exists
        String encodedPassword = passwordEncoder.encode(password);

        // create a new user instance for the admin account and set its properties
        User adminUser = new User();
        adminUser.setCardNumber(cardNumber);
        adminUser.setPassword(encodedPassword);
        adminUser.setRole("ADMIN"); // sets the role to ADMIN
        adminUser.setCreatedAt(java.time.LocalDateTime.now());
        adminUser.setUpdatedAt(java.time.LocalDateTime.now());

        // save the admin user to the database
        User savedAdmin = userRepository.save(adminUser); 

        return ResponseEntity.status(HttpStatus.CREATED).body(savedAdmin);
    }


    // @PostMapping("/inbox/{messageId}/{action}")
    // public ResponseEntity<String> processInboxMessageAction(
    //         @PathVariable String messageId,
    //         @PathVariable String action) {
    //     Optional<InboxMessage> messageOptional = messageService.getMessageById(messageId);
    
    //     if (messageOptional.isEmpty()) {
    //         return ResponseEntity.status(404).body("Message not found");
    //     }
    
    //     InboxMessage message = messageOptional.get();
    
    //     if ("accept".equalsIgnoreCase(action)) {
    //         message.setStatus("ACCEPTED");
    //     } else if ("deny".equalsIgnoreCase(action)) {
    //         message.setStatus("DENIED");
    //     } else {
    //         return ResponseEntity.status(400).body("Invalid action");
    //     }
    
    //     message.setUpdatedAt(LocalDateTime.now());
    //     messageService.saveMessage(message);
    
    //     return ResponseEntity.ok("Message " + action + "ed successfully");
    // }
}