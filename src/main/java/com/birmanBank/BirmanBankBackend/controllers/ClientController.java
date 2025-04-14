// package com.birmanBank.BirmanBankBackend.controllers;

// import com.birmanBank.BirmanBankBackend.models.Client;
// import com.birmanBank.BirmanBankBackend.services.AuthenticationService;
// import com.birmanBank.BirmanBankBackend.services.ClientServices.ClientService;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.web.bind.annotation.*;

// @RestController
// @RequestMapping("/api/clients")
// public class ClientController {

//     private final ClientService clientService;
//     private final AuthenticationService authenticationService;

//     public ClientController(ClientService clientService, AuthenticationService authenticationService) {
//         this.clientService = clientService;
//         this.authenticationService = authenticationService;
//     }

//     @GetMapping("/me")
//     public Client getLoggedInClient(@AuthenticationPrincipal UserDetails userDetails) {
//         // Validate the authenticated user
//         String cardNumber = authenticationService.validateAuthenticatedUser(userDetails);

//         // Fetch the client details using the card number
//         return authenticationService.getAuthenticatedClient(cardNumber);
//     }
// }