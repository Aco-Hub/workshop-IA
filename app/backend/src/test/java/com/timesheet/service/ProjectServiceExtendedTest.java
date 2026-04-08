package com.timesheet.service;

import com.timesheet.dto.ProjectRequest;
import com.timesheet.model.*;
import com.timesheet.repository.ClientRepository;
import com.timesheet.repository.DeveloperRepository;
import com.timesheet.repository.ProjectRepository;
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
class ProjectServiceExtendedTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Developer developer;
    private Client client;
    private Project internalProject;
    private Project externalProject;

    @BeforeEach
    void setUp() {
        developer = Developer.builder()
                .id(1L)
                .email("dev@example.com")
                .password("pass")
                .username("DevUser")
                .role(DeveloperRole.STANDARD)
                .createdAt(LocalDateTime.now())
                .build();

        client = Client.builder()
                .id(1L)
                .name("Acme Corp")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        internalProject = Project.builder()
                .id(1L)
                .name("Internal Project")
                .type(ProjectType.INTERNAL)
                .createdAt(LocalDateTime.now())
                .build();

        externalProject = Project.builder()
                .id(2L)
                .name("External Project")
                .type(ProjectType.EXTERNAL)
                .client(client)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createProject_internalProject_shouldSaveWithoutClient() {
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            final Project p = inv.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        final ProjectRequest request = new ProjectRequest("Internal Project", "INTERNAL", null, null);
        final Project result = projectService.createProject(request, null);

        assertThat(result.getType()).isEqualTo(ProjectType.INTERNAL);
        assertThat(result.getClient()).isNull();
    }

    @Test
    void createProject_externalProjectWithClient_shouldLinkClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            final Project p = inv.getArgument(0);
            p.setId(2L);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        final ProjectRequest request = new ProjectRequest("External Project", "EXTERNAL", null, 1L);
        final Project result = projectService.createProject(request, null);

        assertThat(result.getType()).isEqualTo(ProjectType.EXTERNAL);
        assertThat(result.getClient()).isNotNull();
        assertThat(result.getClient().getName()).isEqualTo("Acme Corp");
    }

    @Test
    void createProject_withCreatorId_creatorIsAutoAssignedToDevelopers() {
        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            final Project p = inv.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        final ProjectRequest request = new ProjectRequest("My Project", "INTERNAL", null, null);
        final Project result = projectService.createProject(request, 1L);

        assertThat(result.getDevelopers()).hasSize(1);
        assertThat(result.getDevelopers().iterator().next().getEmail()).isEqualTo("dev@example.com");
    }

    @Test
    void createProject_externalProjectWithNonExistentClient_shouldThrow() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        final ProjectRequest request = new ProjectRequest("Project", "EXTERNAL", null, 999L);

        assertThatThrownBy(() -> projectService.createProject(request, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    void getAllProjects_shouldReturnAllProjects() {
        when(projectRepository.findAll()).thenReturn(List.of(internalProject, externalProject));

        final List<Project> result = projectService.getAllProjects();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Project::getName)
                .containsExactlyInAnyOrder("Internal Project", "External Project");
    }

    @Test
    void getProjectById_shouldReturnCorrectProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(internalProject));

        final Project result = projectService.getProjectById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Internal Project");
        assertThat(result.getType()).isEqualTo(ProjectType.INTERNAL);
    }

    @Test
    void getProjectById_whenNotFound_shouldThrow() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void updateProject_shouldUpdateNameAndType() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(internalProject));
        when(projectRepository.save(any(Project.class))).thenReturn(internalProject);

        final ProjectRequest request = new ProjectRequest("Renamed Project", "INTERNAL", "https://github.com", null);
        final Project result = projectService.updateProject(1L, request);

        assertThat(internalProject.getName()).isEqualTo("Renamed Project");
        assertThat(internalProject.getRepoUrl()).isEqualTo("https://github.com");
    }

    @Test
    void updateProject_settingClientToNull_shouldClearClient() {
        externalProject.setClient(client);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(externalProject));
        when(projectRepository.save(any(Project.class))).thenReturn(externalProject);

        final ProjectRequest request = new ProjectRequest("External Project", "INTERNAL", null, null);
        projectService.updateProject(2L, request);

        assertThat(externalProject.getClient()).isNull();
    }

    @Test
    void deleteProject_whenExists_shouldDelete() {
        when(projectRepository.existsById(1L)).thenReturn(true);

        projectService.deleteProject(1L);

        verify(projectRepository).deleteById(1L);
    }

    @Test
    void deleteProject_whenNotFound_shouldThrow() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.deleteProject(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void assignDeveloper_shouldAddDeveloperToProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(internalProject));
        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
        when(projectRepository.save(any(Project.class))).thenReturn(internalProject);

        final Project result = projectService.assignDeveloper(1L, 1L);

        assertThat(internalProject.getDevelopers()).contains(developer);
    }

    @Test
    void unassignDeveloper_shouldRemoveDeveloperFromProject() {
        internalProject.getDevelopers().add(developer);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(internalProject));
        when(projectRepository.save(any(Project.class))).thenReturn(internalProject);

        projectService.unassignDeveloper(1L, 1L);

        assertThat(internalProject.getDevelopers()).doesNotContain(developer);
    }

    @Test
    void assignDeveloper_whenProjectNotFound_shouldThrow() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.assignDeveloper(999L, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void assignDeveloper_whenDeveloperNotFound_shouldThrow() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(internalProject));
        when(developerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.assignDeveloper(1L, 999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Developer not found");
    }
}
