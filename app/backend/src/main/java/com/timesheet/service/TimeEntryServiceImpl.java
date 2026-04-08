package com.timesheet.service;

import com.timesheet.dto.RecurringTimeEntryRequest;
import com.timesheet.dto.TimeEntryRequest;
import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import com.timesheet.model.Project;
import com.timesheet.model.TimeEntry;
import com.timesheet.model.TimeEntryType;
import com.timesheet.repository.DeveloperRepository;
import com.timesheet.repository.ProjectRepository;
import com.timesheet.repository.TimeEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeEntryServiceImpl implements TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final DeveloperRepository developerRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimeEntry> getAllTimeEntries(final Long developerId, final Long projectId,
                                             final LocalDateTime startDate, final LocalDateTime endDate) {
        return timeEntryRepository.findWithFilters(developerId, projectId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public TimeEntry getTimeEntryById(final Long id) {
        return timeEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Time entry not found: " + id));
    }

    @Override
    @Transactional
    public TimeEntry createTimeEntry(final TimeEntryRequest request) {
        validateRequest(request);
        verifyOwnership(request.developerId());

        final Developer developer = developerRepository.findById(request.developerId())
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + request.developerId()));

        final TimeEntryType type = parseType(request.type());

        Project project = null;
        if (type == TimeEntryType.WORK) {
            if (request.projectId() == null) {
                throw new IllegalArgumentException("WORK entries must have a project");
            }
            project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.projectId()));

            final Long devId = developer.getId();
            final boolean assigned = project.getDevelopers().stream()
                    .anyMatch(d -> d.getId().equals(devId));
            if (!assigned) {
                throw new IllegalArgumentException("Developer is not assigned to this project");
            }
        }

        final TimeEntry entry = TimeEntry.builder()
                .developer(developer)
                .project(project)
                .type(type)
                .description(request.description())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();

        return timeEntryRepository.save(entry);
    }

    @Override
    @Transactional
    public TimeEntry updateTimeEntry(final Long id, final TimeEntryRequest request) {
        final TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Time entry not found: " + id));

        verifyOwnership(entry.getDeveloper().getId());
        validateRequest(request);

        final TimeEntryType type = parseType(request.type());
        entry.setType(type);
        entry.setDescription(request.description());
        entry.setStartTime(request.startTime());
        entry.setEndTime(request.endTime());

        if (type == TimeEntryType.WORK) {
            if (request.projectId() == null) {
                throw new IllegalArgumentException("WORK entries must have a project");
            }
            final Project project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.projectId()));
            entry.setProject(project);
        } else {
            entry.setProject(null);
        }

        return timeEntryRepository.save(entry);
    }

    @Override
    @Transactional
    public void deleteTimeEntry(final Long id) {
        final TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Time entry not found: " + id));
        verifyOwnership(entry.getDeveloper().getId());
        timeEntryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public List<TimeEntry> createRecurringEntries(final RecurringTimeEntryRequest request) {
        verifyOwnership(request.developerId());

        if (request.endDate() == null && request.count() == null) {
            throw new IllegalArgumentException("Either endDate or count must be provided");
        }

        final Developer developer = developerRepository.findById(request.developerId())
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + request.developerId()));

        final TimeEntryType type = parseType(request.type());

        Project project = null;
        if (type == TimeEntryType.WORK) {
            if (request.projectId() == null) {
                throw new IllegalArgumentException("WORK entries must have a project");
            }
            project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.projectId()));

            final Long devId = developer.getId();
            final boolean assigned = project.getDevelopers().stream()
                    .anyMatch(d -> d.getId().equals(devId));
            if (!assigned) {
                throw new IllegalArgumentException("Developer is not assigned to this project");
            }
        }

        final String frequency = request.frequency().toUpperCase();
        final Duration duration = Duration.between(request.startTime(), request.endTime());
        final UUID groupId = UUID.randomUUID();
        final String recurrenceRule = frequency + (request.endDate() != null
                ? ";UNTIL=" + request.endDate()
                : ";COUNT=" + request.count());

        final int maxInstances = 365;
        final int limit = request.count() != null ? Math.min(request.count(), maxInstances) : maxInstances;

        final List<TimeEntry> entries = new ArrayList<>();
        LocalDate currentDate = request.startTime().toLocalDate();
        int created = 0;

        while (created < limit) {
            if (request.endDate() != null && currentDate.isAfter(request.endDate())) {
                break;
            }

            if (!isSkippedByFrequency(currentDate, frequency)) {
                final LocalDateTime start = currentDate.atTime(request.startTime().toLocalTime());
                final LocalDateTime end = start.plus(duration);

                final TimeEntry entry = TimeEntry.builder()
                        .developer(developer)
                        .project(project)
                        .type(type)
                        .description(request.description())
                        .startTime(start)
                        .endTime(end)
                        .recurrenceGroupId(groupId)
                        .recurrenceRule(recurrenceRule)
                        .build();

                entries.add(entry);
                created++;
            }

            currentDate = advanceDate(currentDate, frequency);
        }

        return timeEntryRepository.saveAll(entries);
    }

    @Override
    @Transactional
    public void deleteByRecurrenceGroup(final UUID groupId) {
        final List<TimeEntry> entries = timeEntryRepository.findByRecurrenceGroupId(groupId);
        if (entries.isEmpty()) {
            throw new EntityNotFoundException("No entries found for recurrence group: " + groupId);
        }
        verifyOwnership(entries.getFirst().getDeveloper().getId());
        timeEntryRepository.deleteByRecurrenceGroupId(groupId);
    }

    @Override
    @Transactional
    public List<TimeEntry> updateByRecurrenceGroup(final UUID groupId, final TimeEntryRequest request) {
        final List<TimeEntry> entries = timeEntryRepository.findByRecurrenceGroupId(groupId);
        if (entries.isEmpty()) {
            throw new EntityNotFoundException("No entries found for recurrence group: " + groupId);
        }
        verifyOwnership(entries.getFirst().getDeveloper().getId());

        final TimeEntryType type = parseType(request.type());

        Project project = null;
        if (type == TimeEntryType.WORK && request.projectId() != null) {
            project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.projectId()));
        }

        for (final TimeEntry entry : entries) {
            entry.setType(type);
            entry.setDescription(request.description());
            entry.setProject(project);
        }

        return timeEntryRepository.saveAll(entries);
    }

    private boolean isSkippedByFrequency(final LocalDate date, final String frequency) {
        if ("DAILY".equals(frequency)) {
            final DayOfWeek dow = date.getDayOfWeek();
            return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        }
        return false;
    }

    private LocalDate advanceDate(final LocalDate date, final String frequency) {
        return switch (frequency) {
            case "DAILY" -> date.plusDays(1);
            case "WEEKLY" -> date.plusWeeks(1);
            case "MONTHLY" -> date.plusMonths(1);
            default -> throw new IllegalArgumentException("Invalid frequency: " + frequency);
        };
    }

    private void verifyOwnership(final Long targetDeveloperId) {
        final Developer currentUser = (Developer) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (currentUser.getRole() == DeveloperRole.ADMIN) {
            return;
        }
        if (!currentUser.getId().equals(targetDeveloperId)) {
            throw new AccessDeniedException("You can only manage your own time entries");
        }
    }

    private void validateRequest(final TimeEntryRequest request) {
        if (request.startTime().isAfter(request.endTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        final long minutes = java.time.Duration.between(request.startTime(), request.endTime()).toMinutes();
        if (minutes % 30 != 0) {
            throw new IllegalArgumentException("Time entries must be in 30-minute intervals");
        }
    }

    private TimeEntryType parseType(final String type) {
        try {
            return TimeEntryType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid time entry type: " + type);
        }
    }
}
