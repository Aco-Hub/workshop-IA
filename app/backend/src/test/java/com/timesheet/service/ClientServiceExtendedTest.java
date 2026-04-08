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
class ClientServiceExtendedTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client clientA;
    private Client clientB;

    @BeforeEach
    void setUp() {
        clientA = Client.builder()
                .id(1L)
                .name("Acme Corp")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        clientB = Client.builder()
                .id(2L)
                .name("Globe Industries")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createClient_shouldPersistClientAndReturnResponse() {
        when(clientRepository.save(any(Client.class))).thenReturn(clientA);

        final Client result = clientService.createClient(new ClientRequest("Acme Corp"));

        assertThat(result.getName()).isEqualTo("Acme Corp");
        assertThat(result.getId()).isEqualTo(1L);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void getAllClients_shouldReturnAllClients() {
        when(clientRepository.findAll()).thenReturn(List.of(clientA, clientB));

        final List<Client> result = clientService.getAllClients();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Client::getName)
                .containsExactlyInAnyOrder("Acme Corp", "Globe Industries");
    }

    @Test
    void getAllClients_whenEmpty_shouldReturnEmptyList() {
        when(clientRepository.findAll()).thenReturn(List.of());

        final List<Client> result = clientService.getAllClients();

        assertThat(result).isEmpty();
    }

    @Test
    void getClientById_shouldReturnCorrectClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientA));

        final Client result = clientService.getClientById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Acme Corp");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void getClientById_whenNotExists_shouldThrowEntityNotFound() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Client not found: 999");
    }

    @Test
    void updateClient_shouldUpdateNameAndReturn() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientA));
        when(clientRepository.save(any(Client.class))).thenReturn(clientA);

        final Client result = clientService.updateClient(1L, new ClientRequest("Renamed Corp"));

        assertThat(clientA.getName()).isEqualTo("Renamed Corp");
        verify(clientRepository).save(clientA);
    }

    @Test
    void updateClient_whenClientNotFound_shouldThrow() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateClient(999L, new ClientRequest("New Name")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteClient_shouldRemoveClient() {
        when(clientRepository.existsById(1L)).thenReturn(true);

        clientService.deleteClient(1L);

        verify(clientRepository).deleteById(1L);
    }

    @Test
    void deleteClient_whenNotFound_shouldThrowEntityNotFound() {
        when(clientRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> clientService.deleteClient(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Client not found: 999");
    }
}
