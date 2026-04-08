package com.timesheet.service;

import com.timesheet.dto.ProjectRequest;
import com.timesheet.model.Client;
import com.timesheet.model.Developer;
import com.timesheet.model.Project;
import com.timesheet.model.ProjectType;
import com.timesheet.repository.ClientRepository;
import com.timesheet.repository.DeveloperRepository;
import com.timesheet.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectById(final Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
    }

    @Override
    @Transactional
    public Project createProject(final ProjectRequest request, final Long creatorId) {
        final Project.ProjectBuilder builder = Project.builder()
                .name(request.name())
                .type(ProjectType.valueOf(request.type()))
                .repoUrl(request.repoUrl());

        if (request.clientId() != null) {
            final Client client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new EntityNotFoundException("Client not found: " + request.clientId()));
            builder.client(client);
        }

        final Project project = builder.build();

        if (creatorId != null) {
            final Developer creator = developerRepository.findById(creatorId)
                    .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + creatorId));
            project.getDevelopers().add(creator);
        }

        return projectRepository.save(project);
    }

    @Override
    @Transactional
    public Project updateProject(final Long id, final ProjectRequest request) {
        final Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));

        project.setName(request.name());
        project.setType(ProjectType.valueOf(request.type()));
        project.setRepoUrl(request.repoUrl());

        if (request.clientId() != null) {
            final Client client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new EntityNotFoundException("Client not found: " + request.clientId()));
            project.setClient(client);
        } else {
            project.setClient(null);
        }

        return projectRepository.save(project);
    }

    @Override
    @Transactional
    public void deleteProject(final Long id) {
        if (!projectRepository.existsById(id)) {
            throw new EntityNotFoundException("Project not found: " + id);
        }
        projectRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Project assignDeveloper(final Long projectId, final Long developerId) {
        final Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
        final Developer developer = developerRepository.findById(developerId)
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + developerId));

        project.getDevelopers().add(developer);
        return projectRepository.save(project);
    }

    @Override
    @Transactional
    public Project unassignDeveloper(final Long projectId, final Long developerId) {
        final Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        project.getDevelopers().removeIf(d -> d.getId().equals(developerId));
        return projectRepository.save(project);
    }
}
