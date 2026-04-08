package com.timesheet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timesheet.config.SecurityConfig;
import com.timesheet.controller.mapper.ClientMapper;
import com.timesheet.dto.ClientRequest;
import com.timesheet.dto.ClientResponse;
import com.timesheet.model.Client;
import com.timesheet.security.JwtAuthenticationFilter;
import com.timesheet.security.JwtTokenProvider;
import com.timesheet.service.ClientService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    @MockBean
    private ClientMapper clientMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.timesheet.repository.DeveloperRepository developerRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    private Client sampleClient() {
        return Client.builder()
                .id(1L)
                .name("Acme Corp")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ClientResponse sampleClientResponse() {
        return new ClientResponse(1L, "Acme Corp", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser
    void createClient_userCanCreateClient() throws Exception {
        final Client client = sampleClient();
        when(clientService.createClient(any(ClientRequest.class))).thenReturn(client);
        when(clientMapper.toClientResponse(client)).thenReturn(sampleClientResponse());

        final ClientRequest request = new ClientRequest("Acme Corp");

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void createClient_withEmptyName_shouldReturn400() throws Exception {
        final ClientRequest badRequest = new ClientRequest("");

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getAllClients_userCanListAllClients() throws Exception {
        final Client clientA = Client.builder().id(1L).name("Client A")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        final Client clientB = Client.builder().id(2L).name("Client B")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        final List<Client> clients = List.of(clientA, clientB);

        when(clientService.getAllClients()).thenReturn(clients);
        when(clientMapper.toClientResponseList(clients)).thenReturn(List.of(
                new ClientResponse(1L, "Client A", LocalDateTime.now(), LocalDateTime.now()),
                new ClientResponse(2L, "Client B", LocalDateTime.now(), LocalDateTime.now())));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Client A"))
                .andExpect(jsonPath("$[1].name").value("Client B"));
    }

    @Test
    @WithMockUser
    void getClientById_userCanGetClientById() throws Exception {
        final Client client = sampleClient();
        when(clientService.getClientById(1L)).thenReturn(client);
        when(clientMapper.toClientResponse(client)).thenReturn(sampleClientResponse());

        mockMvc.perform(get("/api/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Acme Corp"));
    }

    @Test
    @WithMockUser
    void getClientById_whenNotFound_shouldReturn404() throws Exception {
        when(clientService.getClientById(999L))
                .thenThrow(new EntityNotFoundException("Client not found: 999"));

        mockMvc.perform(get("/api/clients/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateClient_userCanUpdateClientName() throws Exception {
        final Client updatedClient = Client.builder().id(1L).name("Updated Corp")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        final ClientResponse updatedResponse = new ClientResponse(1L, "Updated Corp",
                LocalDateTime.now(), LocalDateTime.now());
        when(clientService.updateClient(eq(1L), any(ClientRequest.class))).thenReturn(updatedClient);
        when(clientMapper.toClientResponse(updatedClient)).thenReturn(updatedResponse);

        final ClientRequest request = new ClientRequest("Updated Corp");

        mockMvc.perform(put("/api/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Corp"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteClient_adminCanDeleteClient() throws Exception {
        doNothing().when(clientService).deleteClient(1L);

        mockMvc.perform(delete("/api/clients/1"))
                .andExpect(status().isNoContent());

        verify(clientService).deleteClient(1L);
    }

    @Test
    @WithMockUser(roles = "STANDARD")
    void deleteClient_nonAdminShouldGet403() throws Exception {
        mockMvc.perform(delete("/api/clients/1"))
                .andExpect(status().isForbidden());

        verify(clientService, never()).deleteClient(anyLong());
    }

    @Test
    void getClients_unauthenticatedShouldGet401() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isUnauthorized());
    }
}
