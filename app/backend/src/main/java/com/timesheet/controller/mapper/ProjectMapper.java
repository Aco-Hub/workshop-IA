package com.timesheet.controller.mapper;

import com.timesheet.dto.ProjectResponse;
import com.timesheet.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ClientMapper.class, DeveloperMapper.class})
public interface ProjectMapper {

    @Mapping(target = "type", expression = "java(project.getType().name())")
    @Mapping(source = "developers", target = "assignedDevelopers")
    ProjectResponse toProjectResponse(Project project);

    List<ProjectResponse> toProjectResponseList(List<Project> projects);
}
