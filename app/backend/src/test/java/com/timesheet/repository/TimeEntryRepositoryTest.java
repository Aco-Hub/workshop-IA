package com.timesheet.repository;

import com.timesheet.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TimeEntryRepositoryTest {

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private DeveloperRepository developerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Developer developer;
    private Project project;

    @BeforeEach
    void setUp() {
        developer = developerRepository.save(Developer.builder()
                .email("dev@example.com")
                .password("pass")
                .role(DeveloperRole.STANDARD)
                .build());

        project = projectRepository.save(Project.builder()
                .name("Test Project")
                .type(ProjectType.INTERNAL)
                .build());
    }

    @Test
    void findByDeveloperId_shouldReturnEntries() {
        timeEntryRepository.save(TimeEntry.builder()
                .developer(developer)
                .project(project)
                .type(TimeEntryType.WORK)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusHours(1))
                .build());

        List<TimeEntry> entries = timeEntryRepository.findByDeveloperId(developer.getId());

        assertThat(entries).hasSize(1);
    }

    @Test
    void findWithFilters_byDeveloperId_shouldFilter() {
        timeEntryRepository.save(TimeEntry.builder()
                .developer(developer)
                .type(TimeEntryType.LEAVE)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusHours(1))
                .build());

        List<TimeEntry> entries = timeEntryRepository.findWithFilters(
                developer.getId(), null, null, null);

        assertThat(entries).hasSize(1);
    }

    @Test
    void findWithFilters_withDateRange_shouldFilter() {
        LocalDateTime now = LocalDateTime.now();
        timeEntryRepository.save(TimeEntry.builder()
                .developer(developer)
                .type(TimeEntryType.LEAVE)
                .startTime(now.minusDays(1))
                .endTime(now.minusDays(1).plusHours(1))
                .build());
        timeEntryRepository.save(TimeEntry.builder()
                .developer(developer)
                .type(TimeEntryType.LEAVE)
                .startTime(now.minusDays(10))
                .endTime(now.minusDays(10).plusHours(1))
                .build());

        List<TimeEntry> entries = timeEntryRepository.findWithFilters(
                null, null,
                now.minusDays(3),
                now);

        assertThat(entries).hasSize(1);
    }
}
