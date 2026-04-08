package com.timesheet.service;

import com.timesheet.dto.ClientRequest;
import com.timesheet.model.Client;

import java.util.List;

public interface ClientService {

    List<Client> getAllClients();

    Client getClientById(Long id);

    Client createClient(ClientRequest request);

    Client updateClient(Long id, ClientRequest request);

    void deleteClient(Long id);
}
