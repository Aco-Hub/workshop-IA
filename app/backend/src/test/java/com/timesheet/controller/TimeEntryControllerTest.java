package com.timesheet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timesheet.config.SecurityConfig;
import com.timesheet.controller.mapper.TimeEntryMapper;
import com.timesheet.dto.TimeEntryRequest;
import com.timesheet.dto.TimeEntryResponse;
import com.timesheet.model.*;
import com.timesheet.security.JwtAuthenticationFilter;
import com.timesheet.security.JwtTokenProvider;
import com.timesheet.service.TimeEntryService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeEntryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TimeEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimeEntryService timeEntryService;

    @MockBean
    private TimeEntryMapper timeEntryMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.timesheet.repository.DeveloperRepository developerRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    private LocalDateTime baseTime() {
        return LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).minusHours(2);
    }

    private Developer sampleDeveloper() {
        return Developer.builder()
                .id(1L).email("dev@example.com").username("DevUser")
                .role(DeveloperRole.STANDARD).createdAt(LocalDateTime.now()).build();
    }

    private Project sampleProject() {
        return Project.builder()
                .id(1L).name("Test Project").type(ProjectType.INTERNAL)
                .createdAt(LocalDateTime.now()).build();
    }

    private TimeEntry workEntryEntity() {
        final LocalDateTime start = baseTime();
        return TimeEntry.builder()
                .id(1L).developer(sampleDeveloper()).project(sampleProject())
                .type(TimeEntryType.WORK).description("Feature development")
                .startTime(start).endTime(start.plusHours(1))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private TimeEntryResponse workEntryResponse() {
        final LocalDateTime start = baseTime();
        return new TimeEntryResponse(
                1L, 1L, "DevUser",
                1L, "Test Project",
                "WORK", "Feature development",
                start, start.plusHours(1),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private TimeEntry leaveEntryEntity() {
        final LocalDateTime start = baseTime();
        return TimeEntry.builder()
                .id(2L).developer(sampleDeveloper())
                .type(TimeEntryType.LEAVE).description("Annual leave")
                .startTime(start).endTime(start.plusHours(8))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private TimeEntryResponse leaveEntryResponse() {
        final LocalDateTime start = baseTime();
        return new TimeEntryResponse(
                2L, 1L, "DevUser",
                null, null,
                "LEAVE", "Annual leave",
                start, start.plusHours(8),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser
    void createTimeEntry_userCanCreateWorkEntry() throws Exception {
        final TimeEntry entity = workEntryEntity();
        when(timeEntryService.createTimeEntry(any(TimeEntryRequest.class))).thenReturn(entity);
        when(timeEntryMapper.toTimeEntryResponse(entity)).thenReturn(workEntryResponse());

        final LocalDateTime start = baseTime();
        final TimeEntryRequest request = new TimeEntryRequest(1L, 1L, "WORK", "Feature development",
                start, start.plusHours(1));

        mockMvc.perform(post("/api/time-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WORK"))
                .andExpect(jsonPath("$.projectName").value("Test Project"));
    }

    @Test
    @WithMockUser
    void createTimeEntry_userCanCreateLeaveEntry() throws Exception {
        final TimeEntry entity = leaveEntryEntity();
        when(timeEntryService.createTimeEntry(any(TimeEntryRequest.class))).thenReturn(entity);
        when(timeEntryMapper.toTimeEntryResponse(entity)).thenReturn(leaveEntryResponse());

        final LocalDateTime start = baseTime();
        final TimeEntryRequest request = new TimeEntryRequest(1L, null, "LEAVE", "Annual leave",
                start, start.plusHours(8));

        mockMvc.perform(post("/api/time-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("LEAVE"))
                .andExpect(jsonPath("$.projectId").isEmpty());
    }

    @Test
    @WithMockUser
    void getAllTimeEntries_userCanGetAllEntriesWithoutFilters() throws Exception {
        final List<TimeEntry> entities = List.of(workEntryEntity(), leaveEntryEntity());
        when(timeEntryService.getAllTimeEntries(null, null, null, null)).thenReturn(entities);
        when(timeEntryMapper.toTimeEntryResponseList(entities))
                .thenReturn(List.of(workEntryResponse(), leaveEntryResponse()));

        mockMvc.perform(get("/api/time-entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void getAllTimeEntries_canFilterByDeveloperId() throws Exception {
        final List<TimeEntry> entities = List.of(workEntryEntity());
        when(timeEntryService.getAllTimeEntries(eq(1L), isNull(), isNull(), isNull()))
                .thenReturn(entities);
        when(timeEntryMapper.toTimeEntryResponseList(entities))
                .thenReturn(List.of(workEntryResponse()));

        mockMvc.perform(get("/api/time-entries?developerId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].developerId").value(1));
    }

    @Test
    @WithMockUser
    void getAllTimeEntries_canFilterByProjectId() throws Exception {
        final List<TimeEntry> entities = List.of(workEntryEntity());
        when(timeEntryService.getAllTimeEntries(isNull(), eq(1L), isNull(), isNull()))
                .thenReturn(entities);
        when(timeEntryMapper.toTimeEntryResponseList(entities))
                .thenReturn(List.of(workEntryResponse()));

        mockMvc.perform(get("/api/time-entries?projectId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1));
    }

    @Test
    @WithMockUser
    void getAllTimeEntries_canFilterByDateRange() throws Exception {
        final List<TimeEntry> entities = List.of(workEntryEntity());
        when(timeEntryService.getAllTimeEntries(isNull(), isNull(), any(), any()))
                .thenReturn(entities);
        when(timeEntryMapper.toTimeEntryResponseList(entities))
                .thenReturn(List.of(workEntryResponse()));

        mockMvc.perform(get("/api/time-entries?startDate=2026-01-01T00:00:00&endDate=2026-01-31T23:59:59"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getTimeEntryById_returnsCorrectEntry() throws Exception {
        final TimeEntry entity = workEntryEntity();
        when(timeEntryService.getTimeEntryById(1L)).thenReturn(entity);
        when(timeEntryMapper.toTimeEntryResponse(entity)).thenReturn(workEntryResponse());

        mockMvc.perform(get("/api/time-entries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("WORK"));
    }

    @Test
    @WithMockUser
    void getTimeEntryById_whenNotFound_shouldReturn404() throws Exception {
        when(timeEntryService.getTimeEntryById(999L))
                .thenThrow(new EntityNotFoundException("Time entry not found: 999"));

        mockMvc.perform(get("/api/time-entries/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateTimeEntry_userCanUpdateEntry() throws Exception {
        final LocalDateTime start = baseTime();
        final TimeEntry entity = TimeEntry.builder()
                .id(1L).developer(sampleDeveloper()).project(sampleProject())
                .type(TimeEntryType.WORK).description("Updated description")
                .startTime(start).endTime(start.plusHours(2))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        final TimeEntryResponse updated = new TimeEntryResponse(
                1L, 1L, "DevUser", 1L, "Test Project",
                "WORK", "Updated description",
                start, start.plusHours(2),
                LocalDateTime.now(), LocalDateTime.now());
        when(timeEntryService.updateTimeEntry(eq(1L), any(TimeEntryRequest.class))).thenReturn(entity);
        when(timeEntryMapper.toTimeEntryResponse(entity)).thenReturn(updated);

        final TimeEntryRequest request = new TimeEntryRequest(1L, 1L, "WORK", "Updated description",
                start, start.plusHours(2));

        mockMvc.perform(put("/api/time-entries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @WithMockUser
    void deleteTimeEntry_userCanDeleteEntry() throws Exception {
        doNothing().when(timeEntryService).deleteTimeEntry(1L);

        mockMvc.perform(delete("/api/time-entries/1"))
                .andExpect(status().isNoContent());

        verify(timeEntryService).deleteTimeEntry(1L);
    }

    @Test
    void getAllTimeEntries_unauthenticatedShouldGet401() throws Exception {
        mockMvc.perform(get("/api/time-entries"))
                .andExpect(status().isUnauthorized());
    }
}
