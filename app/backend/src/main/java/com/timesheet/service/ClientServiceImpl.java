package com.timesheet.service;

import com.timesheet.dto.ClientRequest;
import com.timesheet.model.Client;
import com.timesheet.repository.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Client getClientById(final Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
    }

    @Override
    @Transactional
    public Client createClient(final ClientRequest request) {
        final Client client = Client.builder()
                .name(request.name())
                .build();
        return clientRepository.save(client);
    }

    @Override
    @Transactional
    public Client updateClient(final Long id, final ClientRequest request) {
        final Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
        client.setName(request.name());
        return clientRepository.save(client);
    }

    @Override
    @Transactional
    public void deleteClient(final Long id) {
        if (!clientRepository.existsById(id)) {
            throw new EntityNotFoundException("Client not found: " + id);
        }
        clientRepository.deleteById(id);
    }
}
