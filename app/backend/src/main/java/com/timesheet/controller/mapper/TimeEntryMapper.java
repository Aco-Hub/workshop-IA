package com.timesheet.controller.mapper;

import com.timesheet.dto.TimeEntryResponse;
import com.timesheet.model.TimeEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TimeEntryMapper {

    @Mapping(source = "developer.id", target = "developerId")
    @Mapping(source = "developer.displayUsername", target = "developerUsername")
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    @Mapping(target = "type", expression = "java(entry.getType().name())")
    @Mapping(target = "recurrenceGroupId", expression = "java(entry.getRecurrenceGroupId() != null ? entry.getRecurrenceGroupId().toString() : null)")
    @Mapping(source = "recurrenceRule", target = "recurrenceRule")
    TimeEntryResponse toTimeEntryResponse(TimeEntry entry);

    List<TimeEntryResponse> toTimeEntryResponseList(List<TimeEntry> entries);
}
