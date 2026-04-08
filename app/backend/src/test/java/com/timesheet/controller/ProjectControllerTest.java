package com.timesheet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timesheet.config.SecurityConfig;
import com.timesheet.controller.mapper.ProjectMapper;
import com.timesheet.dto.ClientResponse;
import com.timesheet.dto.DeveloperResponse;
import com.timesheet.dto.ProjectRequest;
import com.timesheet.dto.ProjectResponse;
import com.timesheet.model.*;
import com.timesheet.security.JwtAuthenticationFilter;
import com.timesheet.security.JwtTokenProvider;
import com.timesheet.service.ProjectService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ProjectMapper projectMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.timesheet.repository.DeveloperRepository developerRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    private Project internalProjectEntity() {
        return Project.builder()
                .id(1L)
                .name("Internal Project")
                .type(ProjectType.INTERNAL)
                .repoUrl("https://github.com/org/repo")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ProjectResponse internalProjectResponse() {
        return new ProjectResponse(
                1L, "Internal Project", "INTERNAL", "https://github.com/org/repo",
                null, List.of(), LocalDateTime.now(), LocalDateTime.now());
    }

    private Project externalProjectEntity() {
        final Client client = Client.builder()
                .id(1L).name("Acme Corp")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        return Project.builder()
                .id(2L)
                .name("External Project")
                .type(ProjectType.EXTERNAL)
                .client(client)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ProjectResponse externalProjectResponse() {
        final ClientResponse clientResponse =
                new ClientResponse(1L, "Acme Corp", LocalDateTime.now(), LocalDateTime.now());
        return new ProjectResponse(
                2L, "External Project", "EXTERNAL", null,
                clientResponse, List.of(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser
    void createProject_userCanCreateInternalProject() throws Exception {
        final Project entity = internalProjectEntity();
        when(projectService.createProject(any(ProjectRequest.class), isNull())).thenReturn(entity);
        when(projectMapper.toProjectResponse(entity)).thenReturn(internalProjectResponse());

        final ProjectRequest request = new ProjectRequest("Internal Project", "INTERNAL", "https://github.com/org/repo", null);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Internal Project"))
                .andExpect(jsonPath("$.type").value("INTERNAL"));
    }

    @Test
    @WithMockUser
    void createProject_userCanCreateExternalProjectWithClient() throws Exception {
        final Project entity = externalProjectEntity();
        when(projectService.createProject(any(ProjectRequest.class), isNull())).thenReturn(entity);
        when(projectMapper.toProjectResponse(entity)).thenReturn(externalProjectResponse());

        final ProjectRequest request = new ProjectRequest("External Project", "EXTERNAL", null, 1L);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EXTERNAL"))
                .andExpect(jsonPath("$.client.name").value("Acme Corp"));
    }

    @Test
    @WithMockUser
    void createProject_withCreatorId_creatorIsAutoAssigned() throws Exception {
        final Project entity = internalProjectEntity();
        final DeveloperResponse devResponse = new DeveloperResponse(
                1L, "dev@example.com", "DevUser", null, null, null, "STANDARD",
                LocalDateTime.now(), LocalDateTime.now());
        final ProjectResponse projectWithCreator = new ProjectResponse(
                1L, "My Project", "INTERNAL", null,
                null, List.of(devResponse), LocalDateTime.now(), LocalDateTime.now());

        when(projectService.createProject(any(ProjectRequest.class), eq(1L))).thenReturn(entity);
        when(projectMapper.toProjectResponse(entity)).thenReturn(projectWithCreator);

        final ProjectRequest request = new ProjectRequest("My Project", "INTERNAL", null, null);

        mockMvc.perform(post("/api/projects?creatorId=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedDevelopers.length()").value(1));
    }

    @Test
    @WithMockUser
    void getAllProjects_userCanListAllProjects() throws Exception {
        final Project internal = internalProjectEntity();
        final Project external = externalProjectEntity();
        final List<Project> entities = List.of(internal, external);

        when(projectService.getAllProjects()).thenReturn(entities);
        when(projectMapper.toProjectResponseList(entities))
                .thenReturn(List.of(internalProjectResponse(), externalProjectResponse()));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void getProjectById_userCanGetProjectById() throws Exception {
        final Project entity = internalProjectEntity();
        when(projectService.getProjectById(1L)).thenReturn(entity);
        when(projectMapper.toProjectResponse(entity)).thenReturn(internalProjectResponse());

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Internal Project"));
    }

    @Test
    @WithMockUser
    void getProjectById_whenNotFound_shouldReturn404() throws Exception {
        when(projectService.getProjectById(999L))
                .thenThrow(new EntityNotFoundException("Project not found: 999"));

        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateProject_userCanUpdateProject() throws Exception {
        final Project entity = Project.builder()
                .id(1L).name("Renamed Project").type(ProjectType.INTERNAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        final ProjectResponse updated = new ProjectResponse(
                1L, "Renamed Project", "INTERNAL", null,
                null, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(projectService.updateProject(eq(1L), any(ProjectRequest.class))).thenReturn(entity);
        when(projectMapper.toProjectResponse(entity)).thenReturn(updated);

        final ProjectRequest request = new ProjectRequest("Renamed Project", "INTERNAL", null, null);

        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Project"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProject_adminCanDeleteProject() throws Exception {
        doNothing().when(projectService).deleteProject(1L);

        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(1L);
    }

    @Test
    @WithMockUser(roles = "STANDARD")
    void deleteProject_nonAdminShouldGet403() throws Exception {
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isForbidden());

        verify(projectService, never()).deleteProject(anyLong());
    }

    @Test
    @WithMockUser
    void assignDeveloper_userCanAssignThemselveToProject() throws Exception {
        final Project entity = internalProjectEntity();
        final DeveloperResponse devResponse = new DeveloperResponse(
                1L, "dev@example.com", "DevUser", null, null, null, "STANDARD",
                LocalDateTime.now(), LocalDateTime.now());
        final ProjectResponse updatedProject = new ProjectResponse(
                1L, "My Project", "INTERNAL", null,
                null, List.of(devResponse), LocalDateTime.now(), LocalDateTime.now());
        when(projectService.assignDeveloper(1L, 1L)).thenReturn(entity);
        when(projectMapper.toProjectResponse(entity)).thenReturn(updatedProject);

        final Map<String, Long> body = Map.of("developerId", 1L);

        mockMvc.perform(post("/api/projects/1/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedDevelopers.length()").value(1));
    }

    @Test
    @WithMockUser
    void unassignDeveloper_userCanUnassignFromProject() throws Exception {
        final Project entity = internalProjectEntity();
        final ProjectResponse updatedProject = new ProjectResponse(
                1L, "My Project", "INTERNAL", null,
                null, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(projectService.unassignDeveloper(1L, 1L)).thenReturn(entity);
        when(projectMapper.toProjectResponse(entity)).thenReturn(updatedProject);

        final Map<String, Long> body = Map.of("developerId", 1L);

        mockMvc.perform(post("/api/projects/1/unassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedDevelopers.length()").value(0));
    }
}
