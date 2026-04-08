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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceImplTest {

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
    private TimeEntry timeEntry;
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
                .name("Test Project")
                .type(ProjectType.INTERNAL)
                .createdAt(LocalDateTime.now())
                .build();
        project.getDevelopers().add(developer);

        startTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).minusHours(2);
        endTime = startTime.plusHours(1);

        timeEntry = TimeEntry.builder()
                .id(1L)
                .developer(developer)
                .project(project)
                .type(TimeEntryType.WORK)
                .startTime(startTime)
                .endTime(endTime)
                .createdAt(LocalDateTime.now())
                .build();

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
    void getTimeEntryById_whenExists_shouldReturn() {
        when(timeEntryRepository.findById(1L)).thenReturn(Optional.of(timeEntry));

        final TimeEntry result = timeEntryService.getTimeEntryById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(TimeEntryType.WORK);
    }

    @Test
    void getTimeEntryById_whenNotExists_shouldThrow() {
        when(timeEntryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.getTimeEntryById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createTimeEntry_forWork_shouldSave() {
        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(timeEntry);

        final TimeEntryRequest request = new TimeEntryRequest(1L, 1L, "WORK", "Working", startTime, endTime);
        final TimeEntry result = timeEntryService.createTimeEntry(request);

        assertThat(result.getType()).isEqualTo(TimeEntryType.WORK);
        verify(timeEntryRepository).save(any(TimeEntry.class));
    }

    @Test
    void createTimeEntry_withInvalidInterval_shouldThrow() {
        final LocalDateTime badEndTime = startTime.plusMinutes(45);

        final TimeEntryRequest request = new TimeEntryRequest(1L, 1L, "LEAVE", null, startTime, badEndTime);

        assertThatThrownBy(() -> timeEntryService.createTimeEntry(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30-minute");
    }

    @Test
    void createTimeEntry_withStartAfterEnd_shouldThrow() {
        final TimeEntryRequest request = new TimeEntryRequest(1L, null, "LEAVE", null, endTime, startTime);

        assertThatThrownBy(() -> timeEntryService.createTimeEntry(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
    }

    @Test
    void createTimeEntry_forLeave_withoutProject_shouldSucceed() {
        final TimeEntry leaveEntry = TimeEntry.builder()
                .id(2L)
                .developer(developer)
                .type(TimeEntryType.LEAVE)
                .startTime(startTime)
                .endTime(endTime)
                .createdAt(LocalDateTime.now())
                .build();

        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(leaveEntry);

        final TimeEntryRequest request = new TimeEntryRequest(1L, null, "LEAVE", "Annual leave", startTime, endTime);
        final TimeEntry result = timeEntryService.createTimeEntry(request);

        assertThat(result.getType()).isEqualTo(TimeEntryType.LEAVE);
    }

    @Test
    void getAllTimeEntries_shouldDelegateToRepository() {
        when(timeEntryRepository.findWithFilters(null, null, null, null))
                .thenReturn(List.of(timeEntry));

        final List<TimeEntry> result = timeEntryService.getAllTimeEntries(null, null, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void deleteTimeEntry_whenExists_shouldDelete() {
        when(timeEntryRepository.findById(1L)).thenReturn(Optional.of(timeEntry));

        timeEntryService.deleteTimeEntry(1L);

        verify(timeEntryRepository).deleteById(1L);
    }
}
