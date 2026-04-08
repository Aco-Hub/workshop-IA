package com.timesheet.controller;

import com.timesheet.controller.mapper.ClientMapper;
import com.timesheet.dto.ClientRequest;
import com.timesheet.dto.ClientResponse;
import com.timesheet.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        return ResponseEntity.ok(clientMapper.toClientResponseList(clientService.getAllClients()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable final Long id) {
        return ResponseEntity.ok(clientMapper.toClientResponse(clientService.getClientById(id)));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody final ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientMapper.toClientResponse(clientService.createClient(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable final Long id,
            @Valid @RequestBody final ClientRequest request) {
        return ResponseEntity.ok(clientMapper.toClientResponse(clientService.updateClient(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClient(@PathVariable final Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
