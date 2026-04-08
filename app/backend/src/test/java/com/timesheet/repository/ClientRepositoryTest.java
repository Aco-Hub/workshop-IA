package com.timesheet.repository;

import com.timesheet.model.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void save_shouldPersistClient() {
        Client client = Client.builder().name("Acme Corp").build();
        Client saved = clientRepository.save(client);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Acme Corp");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findAll_shouldReturnAllClients() {
        clientRepository.save(Client.builder().name("Client A").build());
        clientRepository.save(Client.builder().name("Client B").build());

        List<Client> clients = clientRepository.findAll();

        assertThat(clients).hasSize(2);
    }
}
