package com.timesheet.service;

import com.timesheet.dto.ProjectRequest;
import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import com.timesheet.model.Project;
import com.timesheet.model.ProjectType;
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
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project project;
    private Developer developer;

    @BeforeEach
    void setUp() {
        developer = Developer.builder()
                .id(1L)
                .email("dev@example.com")
                .password("pass")
                .role(DeveloperRole.STANDARD)
                .createdAt(LocalDateTime.now())
                .build();

        project = Project.builder()
                .id(1L)
                .name("Test Project")
                .type(ProjectType.INTERNAL)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllProjects_shouldReturnAll() {
        when(projectRepository.findAll()).thenReturn(List.of(project));

        final List<Project> result = projectService.getAllProjects();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Project");
    }

    @Test
    void getProjectById_whenExists_shouldReturn() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        final Project result = projectService.getProjectById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(ProjectType.INTERNAL);
    }

    @Test
    void getProjectById_whenNotExists_shouldThrow() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createProject_withoutCreator_shouldSave() {
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        final ProjectRequest request = new ProjectRequest("Test Project", "INTERNAL", null, null);
        final Project result = projectService.createProject(request, null);

        assertThat(result.getName()).isEqualTo("Test Project");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProject_withCreator_shouldAssignDeveloper() {
        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            final Project p = inv.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        final ProjectRequest request = new ProjectRequest("Test Project", "INTERNAL", null, null);
        final Project result = projectService.createProject(request, 1L);

        assertThat(result.getDevelopers()).hasSize(1);
    }

    @Test
    void assignDeveloper_shouldAddToProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        project.getDevelopers().add(developer);

        final Project result = projectService.assignDeveloper(1L, 1L);

        assertThat(result).isNotNull();
    }

    @Test
    void deleteProject_whenNotExists_shouldThrow() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.deleteProject(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
