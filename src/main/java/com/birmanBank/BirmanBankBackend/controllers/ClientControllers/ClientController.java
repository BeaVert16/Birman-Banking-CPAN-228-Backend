package com.birmanBank.BirmanBankBackend.controllers.ClientControllers;

import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.services.ClientServices.ClientService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    
    private ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    //me is a JWT endpoint used to return information based on the logged-in client
    //used to get logged-in client details using JWT /me endpoint
    //endpoint returns the details of the logged-in client
    @GetMapping("/me")
    public Optional<Client> getLoggedInClient(Authentication authentication) {
        String userCardNumber = authentication.getName();
        return clientService.getClientByUserCardNumber(userCardNumber);
    }
}