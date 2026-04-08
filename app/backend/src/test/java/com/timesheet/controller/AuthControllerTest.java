package com.timesheet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timesheet.config.SecurityConfig;
import com.timesheet.controller.mapper.DeveloperMapper;
import com.timesheet.dto.AuthResult;
import com.timesheet.dto.LoginRequest;
import com.timesheet.dto.LoginResponse;
import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import com.timesheet.security.JwtAuthenticationFilter;
import com.timesheet.security.JwtTokenProvider;
import com.timesheet.service.AuthService;
import com.timesheet.service.DiscordOAuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private DiscordOAuthService discordOAuthService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.timesheet.repository.DeveloperRepository developerRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private DeveloperMapper developerMapper;

    @Test
    void login_withValidCredentials_shouldReturnToken() throws Exception {
        final LocalDateTime now = LocalDateTime.now();
        final Developer developer = Developer.builder()
                .id(1L)
                .email("test@example.com")
                .username("TestUser")
                .role(DeveloperRole.STANDARD)
                .createdAt(now)
                .updatedAt(now)
                .build();
        final AuthResult authResult = new AuthResult(developer, "test-jwt-token");
        final LoginResponse loginResponse = new LoginResponse(
                "test-jwt-token", 1L, "test@example.com", "TestUser",
                null, null, null, "STANDARD", now, now);

        when(authService.login(any(LoginRequest.class), anyString()))
                .thenReturn(authResult);
        when(developerMapper.toLoginResponse(developer, "test-jwt-token"))
                .thenReturn(loginResponse);

        final LoginRequest request = new LoginRequest("test@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_withInvalidRequest_shouldReturn400() throws Exception {
        final LoginRequest badRequest = new LoginRequest("not-an-email", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void logout_whenAuthenticated_shouldReturn204() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void me_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
