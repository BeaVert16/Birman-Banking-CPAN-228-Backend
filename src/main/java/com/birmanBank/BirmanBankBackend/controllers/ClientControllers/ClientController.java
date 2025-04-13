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

    @GetMapping("/me")
    public Optional<Client> getLoggedInClient(Authentication authentication) {
        String userCardNumber = authentication.getName();
        return clientService.getClientByUserCardNumber(userCardNumber);
    }
}