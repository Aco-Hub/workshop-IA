package com.timesheet.service;

import com.timesheet.dto.ProjectRequest;
import com.timesheet.model.Project;

import java.util.List;

public interface ProjectService {

    List<Project> getAllProjects();

    Project getProjectById(Long id);

    Project createProject(ProjectRequest request, Long creatorId);

    Project updateProject(Long id, ProjectRequest request);

    void deleteProject(Long id);

    Project assignDeveloper(Long projectId, Long developerId);

    Project unassignDeveloper(Long projectId, Long developerId);
}
