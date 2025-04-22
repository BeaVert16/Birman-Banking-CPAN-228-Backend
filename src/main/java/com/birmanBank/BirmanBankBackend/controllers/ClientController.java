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

/*
* ClientController handles client-related operations
* fetching client details, inbox messages, etc.
*/

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    //-----------------------Constructors----------------------//
    private final ClientService clientService;
    private final AuthenticationService authenticationService;
    private final InboxMessageRepository inboxMessageRepository;

    public ClientController(ClientService clientService, AuthenticationService authenticationService,
            InboxMessageRepository inboxMessageRepository) {
        this.clientService = clientService;
        this.authenticationService = authenticationService;
        this.inboxMessageRepository = inboxMessageRepository;
    }
    // ---------------------------------------------------------------//

    // ednpoint to fetch client details
    // currently not used - may be moved to account controller for fetching account details for client 
    @GetMapping("/me")
    public Client getLoggedInClient(@AuthenticationPrincipal UserDetails userDetails) {
        // fetch the client details using the card number
        return clientService.getAuthenticatedClient(userDetails, authenticationService);
    }

    // endpoint to fetch inbox messages for the logged-in client
    @GetMapping("/inbox")
    public ResponseEntity<List<InboxMessage>> getInboxMessages(@AuthenticationPrincipal UserDetails userDetails) {

        Client client = clientService.getAuthenticatedClient(userDetails, authenticationService); // validate the authenticated user
        List<InboxMessage> messages = inboxMessageRepository.findByRecipientId(client.getUserCardNumber()); // fetch inbox messages for the authenticated user
        return ResponseEntity.ok(messages);
    }
    
    
}