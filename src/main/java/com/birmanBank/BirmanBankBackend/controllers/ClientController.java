package com.birmanBank.BirmanBankBackend.controllers;

import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.models.InboxMessage;
import com.birmanBank.BirmanBankBackend.services.AuthenticationService;
import com.birmanBank.BirmanBankBackend.services.ClientServices.ClientService;

import com.birmanBank.BirmanBankBackend.repositories.InboxMessageRepository;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final AuthenticationService authenticationService;
    private final InboxMessageRepository inboxMessageRepository;

    public ClientController(ClientService clientService, AuthenticationService authenticationService,
            InboxMessageRepository inboxMessageRepository) {
        this.clientService = clientService;
        this.authenticationService = authenticationService;
        this.inboxMessageRepository = inboxMessageRepository;
    }

    @GetMapping("/me")
    public Client getLoggedInClient(@AuthenticationPrincipal UserDetails userDetails) {
        // Validate the authenticated user
        String cardNumber = authenticationService.validateAuthenticatedUser(userDetails);

        // Fetch the client details using the card number
        return authenticationService.getAuthenticatedClient(cardNumber);
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<InboxMessage>> getInboxMessages(@AuthenticationPrincipal UserDetails userDetails) {
        String cardNumber = authenticationService.validateAuthenticatedUser(userDetails);
        List<InboxMessage> messages = inboxMessageRepository.findByRecipientId(cardNumber);
        return ResponseEntity.ok(messages);
    }
}