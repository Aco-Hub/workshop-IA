package com.timesheet.service;

import com.timesheet.dto.ClientRequest;
import com.timesheet.model.Client;
import com.timesheet.repository.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client client;

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .id(1L)
                .name("Acme Corp")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllClients_shouldReturnAllClients() {
        when(clientRepository.findAll()).thenReturn(List.of(client));

        final List<Client> result = clientService.getAllClients();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Acme Corp");
    }

    @Test
    void getClientById_whenExists_shouldReturnClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        final Client result = clientService.getClientById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Acme Corp");
    }

    @Test
    void getClientById_whenNotExists_shouldThrow() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createClient_shouldSaveAndReturnClient() {
        when(clientRepository.save(any(Client.class))).thenReturn(client);

        final Client result = clientService.createClient(new ClientRequest("Acme Corp"));

        assertThat(result.getName()).isEqualTo("Acme Corp");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void deleteClient_whenExists_shouldDelete() {
        when(clientRepository.existsById(1L)).thenReturn(true);

        clientService.deleteClient(1L);

        verify(clientRepository).deleteById(1L);
    }

    @Test
    void deleteClient_whenNotExists_shouldThrow() {
        when(clientRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> clientService.deleteClient(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
