package com.timesheet.repository;

import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import com.timesheet.model.Project;
import com.timesheet.model.ProjectType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DeveloperRepository developerRepository;

    @Test
    void findByDevelopersId_shouldReturnAssignedProjects() {
        Developer developer = developerRepository.save(Developer.builder()
                .email("dev@example.com")
                .password("pass")
                .role(DeveloperRole.STANDARD)
                .build());

        Project project = Project.builder()
                .name("Test Project")
                .type(ProjectType.INTERNAL)
                .build();
        project.getDevelopers().add(developer);
        projectRepository.save(project);

        List<Project> projects = projectRepository.findByDevelopersId(developer.getId());

        assertThat(projects).hasSize(1);
        assertThat(projects.get(0).getName()).isEqualTo("Test Project");
    }

    @Test
    void findByDevelopersId_whenNotAssigned_shouldReturnEmpty() {
        Developer developer = developerRepository.save(Developer.builder()
                .email("unassigned@example.com")
                .password("pass")
                .role(DeveloperRole.STANDARD)
                .build());

        List<Project> projects = projectRepository.findByDevelopersId(developer.getId());

        assertThat(projects).isEmpty();
    }
}
