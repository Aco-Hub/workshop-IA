package com.timesheet.service;

import com.timesheet.dto.TimeEntryRequest;
import com.timesheet.model.*;
import com.timesheet.repository.DeveloperRepository;
import com.timesheet.repository.ProjectRepository;
import com.timesheet.repository.TimeEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceExtendedTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TimeEntryServiceImpl timeEntryService;

    private Developer developer;
    private Project project;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

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
                .name("My Project")
                .type(ProjectType.INTERNAL)
                .createdAt(LocalDateTime.now())
                .build();
        project.getDevelopers().add(developer);

        startTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).minusHours(3);
        endTime = startTime.plusHours(1);

        // Set up SecurityContext with the developer as the authenticated user
        final UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(developer, null, developer.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTimeEntry_workEntryRequiresProject_throwsWhenProjectIdIsNull() {
        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));

        final TimeEntryRequest request = new TimeEntryRequest(1L, null, "WORK", "Working", startTime, endTime);

        assertThatThrownBy(() -> timeEntryService.createTimeEntry(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WORK entries must have a project");
    }

    @Test
    void createTimeEntry_developerMustBeAssignedToProjectToLogWork() {
        final Developer unassignedDev = Developer.builder()
                .id(2L)
                .email("other@example.com")
                .password("pass")
                .role(DeveloperRole.STANDARD)
                .createdAt(LocalDateTime.now())
                .build();

        // Set SecurityContext to the unassigned developer
        final UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(unassignedDev, null, unassignedDev.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final Project projectWithoutDev = Project.builder()
                .id(2L)
                .name("Other Project")
                .type(ProjectType.INTERNAL)
                .createdAt(LocalDateTime.now())
                .build();

        when(developerRepository.findById(2L)).thenReturn(Optional.of(unassignedDev));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(projectWithoutDev));

        final TimeEntryRequest request = new TimeEntryRequest(2L, 2L, "WORK", "Trying to work", startTime, endTime);

        assertThatThrownBy(() -> timeEntryService.createTimeEntry(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not assigned to this project");
    }

    @Test
    void createTimeEntry_leaveEntryDoesNotRequireProject() {
        final TimeEntry leaveEntry = TimeEntry.builder()
                .id(3L)
                .developer(developer)
                .type(TimeEntryType.LEAVE)
                .startTime(startTime)
                .endTime(endTime)
                .createdAt(LocalDateTime.now())
                .build();

        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(leaveEntry);

        final TimeEntryRequest request = new TimeEntryRequest(1L, null, "LEAVE", "Leave day", startTime, endTime);
        final TimeEntry result = timeEntryService.createTimeEntry(request);

        assertThat(result.getType()).isEqualTo(TimeEntryType.LEAVE);
        assertThat(result.getProject()).isNull();
        verify(projectRepository, never()).findById(anyLong());
    }

    @Test
    void createTimeEntry_startTimeMustBeBeforeEndTime() {
        final TimeEntryRequest request = new TimeEntryRequest(1L, null, "LEAVE", "Bad times", endTime, startTime);

        assertThatThrownBy(() -> timeEntryService.createTimeEntry(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before end time");
    }

    @Test
    void createTimeEntry_entriesMustBeIn30MinuteIntervals_rejectOddMinutes() {
        final LocalDateTime badEnd = startTime.plusMinutes(25);

        final TimeEntryRequest request = new TimeEntryRequest(1L, null, "LEAVE", "Wrong interval", startTime, badEnd);

        assertThatThrownBy(() -> timeEntryService.createTimeEntry(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30-minute");
    }

    @Test
    void createTimeEntry_exactly30MinutesIsValid() {
        final LocalDateTime thirtyMinEnd = startTime.plusMinutes(30);
        final TimeEntry entry = TimeEntry.builder()
                .id(4L)
                .developer(developer)
                .project(project)
                .type(TimeEntryType.WORK)
                .startTime(startTime)
                .endTime(thirtyMinEnd)
                .createdAt(LocalDateTime.now())
                .build();

        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(entry);

        final TimeEntryRequest request = new TimeEntryRequest(1L, 1L, "WORK", "Short work", startTime, thirtyMinEnd);
        final TimeEntry result = timeEntryService.createTimeEntry(request);

        assertThat(result).isNotNull();
    }

    @Test
    void createTimeEntry_exactly90MinutesIsValid() {
        final LocalDateTime ninetyMinEnd = startTime.plusMinutes(90);
        final TimeEntry entry = TimeEntry.builder()
                .id(5L)
                .developer(developer)
                .project(project)
                .type(TimeEntryType.WORK)
                .startTime(startTime)
                .endTime(ninetyMinEnd)
                .createdAt(LocalDateTime.now())
                .build();

        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(entry);

        final TimeEntryRequest request = new TimeEntryRequest(1L, 1L, "WORK", "1.5 hours", startTime, ninetyMinEnd);
        final TimeEntry result = timeEntryService.createTimeEntry(request);

        assertThat(result).isNotNull();
    }

    @Test
    void updateTimeEntry_whenEntryExists_shouldUpdateFields() {
        final TimeEntry existingEntry = TimeEntry.builder()
                .id(1L)
                .developer(developer)
                .project(project)
                .type(TimeEntryType.WORK)
                .description("Old description")
                .startTime(startTime)
                .endTime(endTime)
                .createdAt(LocalDateTime.now())
                .build();

        final TimeEntry updatedEntry = TimeEntry.builder()
                .id(1L)
                .developer(developer)
                .project(project)
                .type(TimeEntryType.WORK)
                .description("New description")
                .startTime(startTime)
                .endTime(endTime)
                .createdAt(LocalDateTime.now())
                .build();

        when(timeEntryRepository.findById(1L)).thenReturn(Optional.of(existingEntry));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(updatedEntry);

        final TimeEntryRequest request = new TimeEntryRequest(1L, 1L, "WORK", "New description", startTime, endTime);
        final TimeEntry result = timeEntryService.updateTimeEntry(1L, request);

        assertThat(result.getDescription()).isEqualTo("New description");
    }

    @Test
    void deleteTimeEntry_whenExists_shouldDelete() {
        final TimeEntry entry = TimeEntry.builder()
                .id(1L).developer(developer).project(project)
                .type(TimeEntryType.WORK).startTime(startTime).endTime(endTime)
                .createdAt(LocalDateTime.now()).build();
        when(timeEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        timeEntryService.deleteTimeEntry(1L);

        verify(timeEntryRepository).deleteById(1L);
    }

    @Test
    void deleteTimeEntry_whenNotFound_shouldThrow() {
        when(timeEntryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.deleteTimeEntry(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getAllTimeEntries_canFilterByDeveloper() {
        final TimeEntry entry = TimeEntry.builder()
                .id(1L).developer(developer).project(project)
                .type(TimeEntryType.WORK)
                .startTime(startTime).endTime(endTime)
                .createdAt(LocalDateTime.now())
                .build();

        when(timeEntryRepository.findWithFilters(1L, null, null, null)).thenReturn(List.of(entry));

        final List<TimeEntry> result = timeEntryService.getAllTimeEntries(1L, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeveloper().getId()).isEqualTo(1L);
    }

    @Test
    void getAllTimeEntries_canFilterByProject() {
        final TimeEntry entry = TimeEntry.builder()
                .id(1L).developer(developer).project(project)
                .type(TimeEntryType.WORK)
                .startTime(startTime).endTime(endTime)
                .createdAt(LocalDateTime.now())
                .build();

        when(timeEntryRepository.findWithFilters(null, 1L, null, null)).thenReturn(List.of(entry));

        final List<TimeEntry> result = timeEntryService.getAllTimeEntries(null, 1L, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProject().getId()).isEqualTo(1L);
    }

    @Test
    void getAllTimeEntries_canFilterByDateRange() {
        final LocalDateTime rangeStart = startTime.minusDays(1);
        final LocalDateTime rangeEnd = endTime.plusDays(1);

        final TimeEntry entry = TimeEntry.builder()
                .id(1L).developer(developer).project(project)
                .type(TimeEntryType.WORK)
                .startTime(startTime).endTime(endTime)
                .createdAt(LocalDateTime.now())
                .build();

        when(timeEntryRepository.findWithFilters(null, null, rangeStart, rangeEnd)).thenReturn(List.of(entry));

        final List<TimeEntry> result = timeEntryService.getAllTimeEntries(null, null, rangeStart, rangeEnd);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllTimeEntries_withNoFilters_returnsAllEntries() {
        when(timeEntryRepository.findWithFilters(null, null, null, null)).thenReturn(List.of());

        final List<TimeEntry> result = timeEntryService.getAllTimeEntries(null, null, null, null);

        assertThat(result).isEmpty();
        verify(timeEntryRepository).findWithFilters(null, null, null, null);
    }
}
