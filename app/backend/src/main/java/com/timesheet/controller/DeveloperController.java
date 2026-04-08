package com.timesheet.controller;

import com.timesheet.controller.mapper.DeveloperMapper;
import com.timesheet.dto.DeveloperResponse;
import com.timesheet.dto.InviteRequest;
import com.timesheet.dto.InviteResponse;
import com.timesheet.service.DeveloperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/developers")
@RequiredArgsConstructor
public class DeveloperController {

    private final DeveloperService developerService;
    private final DeveloperMapper developerMapper;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    @GetMapping
    public ResponseEntity<List<DeveloperResponse>> getAllDevelopers() {
        return ResponseEntity.ok(developerMapper.toDeveloperResponseList(developerService.getAllDevelopers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeveloperResponse> getDeveloperById(@PathVariable final Long id) {
        return ResponseEntity.ok(developerMapper.toDeveloperResponse(developerService.getDeveloperById(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDeveloper(@PathVariable final Long id) {
        developerService.deleteDeveloper(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InviteResponse> inviteDeveloper(
            @Valid @RequestBody final InviteRequest request) {
        return ResponseEntity.ok(developerService.inviteDeveloper(request, baseUrl));
    }
}
