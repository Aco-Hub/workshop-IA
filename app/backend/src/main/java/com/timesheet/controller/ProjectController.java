package com.timesheet.controller;

import com.timesheet.controller.mapper.ProjectMapper;
import com.timesheet.dto.ProjectRequest;
import com.timesheet.dto.ProjectResponse;
import com.timesheet.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectMapper.toProjectResponseList(projectService.getAllProjects()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable final Long id) {
        return ResponseEntity.ok(projectMapper.toProjectResponse(projectService.getProjectById(id)));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody final ProjectRequest request,
            @RequestParam(required = false) final Long creatorId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectMapper.toProjectResponse(projectService.createProject(request, creatorId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable final Long id,
            @Valid @RequestBody final ProjectRequest request) {
        return ResponseEntity.ok(projectMapper.toProjectResponse(projectService.updateProject(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProject(@PathVariable final Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ProjectResponse> assignDeveloper(
            @PathVariable final Long id,
            @RequestBody final Map<String, Long> body) {
        final Long developerId = body.get("developerId");
        if (developerId == null) {
            throw new IllegalArgumentException("developerId is required");
        }
        return ResponseEntity.ok(projectMapper.toProjectResponse(projectService.assignDeveloper(id, developerId)));
    }

    @PostMapping("/{id}/unassign")
    public ResponseEntity<ProjectResponse> unassignDeveloper(
            @PathVariable final Long id,
            @RequestBody final Map<String, Long> body) {
        final Long developerId = body.get("developerId");
        if (developerId == null) {
            throw new IllegalArgumentException("developerId is required");
        }
        return ResponseEntity.ok(projectMapper.toProjectResponse(projectService.unassignDeveloper(id, developerId)));
    }
}
