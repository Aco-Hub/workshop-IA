package com.timesheet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timesheet.config.SecurityConfig;
import com.timesheet.controller.mapper.DeveloperMapper;
import com.timesheet.dto.DeveloperResponse;
import com.timesheet.dto.InviteRequest;
import com.timesheet.dto.InviteResponse;
import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import com.timesheet.security.JwtAuthenticationFilter;
import com.timesheet.security.JwtTokenProvider;
import com.timesheet.service.DeveloperService;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeveloperController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DeveloperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeveloperService developerService;

    @MockBean
    private DeveloperMapper developerMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.timesheet.repository.DeveloperRepository developerRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    private Developer sampleDeveloper() {
        return Developer.builder()
                .id(1L)
                .email("dev@example.com")
                .username("DevUser")
                .title("Senior Dev")
                .role(DeveloperRole.STANDARD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private DeveloperResponse sampleDeveloperResponse() {
        return new DeveloperResponse(
                1L, "dev@example.com", "DevUser",
                "Senior Dev", null, null, "STANDARD",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllDevelopers_adminCanListAllDevelopers() throws Exception {
        final List<Developer> developers = List.of(sampleDeveloper());
        when(developerService.getAllDevelopers()).thenReturn(developers);
        when(developerMapper.toDeveloperResponseList(developers))
                .thenReturn(List.of(sampleDeveloperResponse()));

        mockMvc.perform(get("/api/developers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("dev@example.com"))
                .andExpect(jsonPath("$[0].role").value("STANDARD"));
    }

    @Test
    @WithMockUser(roles = "STANDARD")
    void getAllDevelopers_standardUserCanAlsoListDevelopers() throws Exception {
        final List<Developer> developers = List.of(sampleDeveloper());
        when(developerService.getAllDevelopers()).thenReturn(developers);
        when(developerMapper.toDeveloperResponseList(developers))
                .thenReturn(List.of(sampleDeveloperResponse()));

        mockMvc.perform(get("/api/developers"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDeveloperById_adminCanGetDeveloperById() throws Exception {
        final Developer developer = sampleDeveloper();
        when(developerService.getDeveloperById(1L)).thenReturn(developer);
        when(developerMapper.toDeveloperResponse(developer)).thenReturn(sampleDeveloperResponse());

        mockMvc.perform(get("/api/developers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("dev@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDeveloperById_whenNotFound_shouldReturn404() throws Exception {
        when(developerService.getDeveloperById(999L))
                .thenThrow(new EntityNotFoundException("Developer not found: 999"));

        mockMvc.perform(get("/api/developers/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDeveloper_adminCanDeleteDeveloper() throws Exception {
        doNothing().when(developerService).deleteDeveloper(1L);

        mockMvc.perform(delete("/api/developers/1"))
                .andExpect(status().isNoContent());

        verify(developerService).deleteDeveloper(1L);
    }

    @Test
    @WithMockUser(roles = "STANDARD")
    void deleteDeveloper_nonAdminShouldGet403() throws Exception {
        mockMvc.perform(delete("/api/developers/1"))
                .andExpect(status().isForbidden());

        verify(developerService, never()).deleteDeveloper(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void inviteDeveloper_adminCanInviteNewDeveloper() throws Exception {
        final InviteResponse inviteResponse = new InviteResponse(
                "http://localhost:5173/register?token=abc123",
                "Invite email sent to newdev@example.com");
        when(developerService.inviteDeveloper(any(InviteRequest.class), anyString()))
                .thenReturn(inviteResponse);

        final InviteRequest request = new InviteRequest("newdev@example.com");

        mockMvc.perform(post("/api/developers/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteLink").value(containsString("register?token=")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void inviteDeveloper_generatedLinkIsValid() throws Exception {
        final InviteResponse inviteResponse = new InviteResponse(
                "http://localhost:5173/register?token=real-jwt-token",
                "Invite email sent to dev@company.com");
        when(developerService.inviteDeveloper(any(InviteRequest.class), anyString()))
                .thenReturn(inviteResponse);

        final InviteRequest request = new InviteRequest("dev@company.com");

        mockMvc.perform(post("/api/developers/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteLink").value("http://localhost:5173/register?token=real-jwt-token"));
    }

    @Test
    @WithMockUser(roles = "STANDARD")
    void inviteDeveloper_nonAdminShouldGet403() throws Exception {
        final InviteRequest request = new InviteRequest("dev@example.com");

        mockMvc.perform(post("/api/developers/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllDevelopers_unauthenticatedShouldGet401() throws Exception {
        mockMvc.perform(get("/api/developers"))
                .andExpect(status().isUnauthorized());
    }
}
