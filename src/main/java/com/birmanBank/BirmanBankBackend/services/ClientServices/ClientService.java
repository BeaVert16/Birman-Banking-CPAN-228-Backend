package com.birmanBank.BirmanBankBackend.services.ClientServices;

import org.springframework.stereotype.Service;

import com.birmanBank.BirmanBankBackend.models.Client;
import com.birmanBank.BirmanBankBackend.repositories.ClientRepositories.ClientRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    //-----------------------Constructors----------------------//
    private ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    } 
    //---------------------------------------------------------------//

    //create a new client
    public Client createClient(Client client) {
        return clientRepository.save(client);
    }
    
    //retrieve a client by its ID
    public Optional<Client> getClientById(String clientId) {
        return clientRepository.findById(clientId);
    }
    
    //retrieve a client by its user card number
    public Optional<Client> getClientByUserCardNumber(String userCardNumber) {
        return clientRepository.findByUserCardNumber(userCardNumber);
    }
    
    //retrieve all clients
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }
    
    // //update a client
    // public Client updateClient(Client client) {
    //     return clientRepository.save(client);
    // }
    
    // // delete a client by its ID
    // public void deleteClient(String clientId) {
    //     clientRepository.deleteById(clientId);
    // }
}
