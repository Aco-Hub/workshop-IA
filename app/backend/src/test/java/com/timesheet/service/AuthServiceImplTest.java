package com.timesheet.service;

import com.timesheet.dto.*;
import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import com.timesheet.repository.DeveloperRepository;
import com.timesheet.security.JwtTokenProvider;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Developer developer;

    @BeforeEach
    void setUp() {
        developer = Developer.builder()
                .id(1L)
                .email("dev@example.com")
                .password("encoded-password")
                .username("DevUser")
                .title("Senior Developer")
                .discordLink("https://discord.gg/test")
                .role(DeveloperRole.STANDARD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- Login tests ---

    @Test
    void login_withValidCredentials_shouldReturnTokenAndDeveloperInfo() {
        final Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(developer);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtTokenProvider.generateToken("dev@example.com", "127.0.0.1"))
                .thenReturn("valid-jwt-token");

        final LoginRequest request = new LoginRequest("dev@example.com", "password123");
        final AuthResult result = authService.login(request, "127.0.0.1");

        assertThat(result.token()).isEqualTo("valid-jwt-token");
        assertThat(result.developer().getEmail()).isEqualTo("dev@example.com");
        assertThat(result.developer().getId()).isEqualTo(1L);
        assertThat(result.developer().getRole()).isEqualTo(DeveloperRole.STANDARD);
    }

    @Test
    void login_withWrongPassword_shouldThrowBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        final LoginRequest request = new LoginRequest("dev@example.com", "wrong-password");

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withNonExistentEmail_shouldThrowBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User not found"));

        final LoginRequest request = new LoginRequest("unknown@example.com", "password123");

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);
    }

    // --- Register tests ---

    @Test
    void register_withValidInviteToken_shouldCreateDeveloperAndReturnToken() {
        when(jwtTokenProvider.validateInviteToken("valid-invite-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-invite-token")).thenReturn("newdev@example.com");
        when(developerRepository.existsByEmail("newdev@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePass1")).thenReturn("encoded-pass");
        when(developerRepository.save(any(Developer.class))).thenAnswer(invocation -> {
            final Developer saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });
        when(jwtTokenProvider.generateToken(anyString(), anyString())).thenReturn("new-jwt-token");

        final RegisterRequest request = new RegisterRequest(
                "valid-invite-token", "NewDev", "securePass1", "Junior Dev", null);
        final AuthResult result = authService.register(request, "127.0.0.1");

        assertThat(result.token()).isEqualTo("new-jwt-token");
        assertThat(result.developer().getEmail()).isEqualTo("newdev@example.com");
        assertThat(result.developer().getRole()).isEqualTo(DeveloperRole.STANDARD);
        verify(developerRepository).save(any(Developer.class));
    }

    @Test
    void register_withExpiredOrInvalidToken_shouldThrowIllegalArgument() {
        when(jwtTokenProvider.validateInviteToken("expired-token")).thenReturn(false);

        final RegisterRequest request = new RegisterRequest(
                "expired-token", "NewDev", "securePass1", null, null);

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired invite token");
    }

    @Test
    void register_withInvalidToken_shouldThrowIllegalArgument() {
        when(jwtTokenProvider.validateInviteToken("garbage-token")).thenReturn(false);

        final RegisterRequest request = new RegisterRequest(
                "garbage-token", "Dev", "securePass1", null, null);

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_withAlreadyExistingEmail_shouldThrowIllegalState() {
        when(jwtTokenProvider.validateInviteToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-token")).thenReturn("existing@example.com");
        when(developerRepository.existsByEmail("existing@example.com")).thenReturn(true);

        final RegisterRequest request = new RegisterRequest(
                "valid-token", "Dev", "securePass1", null, null);

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    // --- getCurrentDeveloper tests ---

    @Test
    void getCurrentDeveloper_whenEmailExists_shouldReturnDeveloper() {
        when(developerRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer));

        final Developer result = authService.getCurrentDeveloper("dev@example.com");

        assertThat(result.getEmail()).isEqualTo("dev@example.com");
    }

    @Test
    void getCurrentDeveloper_whenEmailNotFound_shouldThrowEntityNotFound() {
        when(developerRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentDeveloper("unknown@example.com"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- updateProfile tests ---

    @Test
    void updateProfile_updatingUsername_shouldPersistNewUsername() {
        when(developerRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer));
        when(developerRepository.save(any(Developer.class))).thenReturn(developer);

        final ProfileUpdateRequest request = new ProfileUpdateRequest("NewUsername", null, null);
        authService.updateProfile("dev@example.com", request);

        assertThat(developer.getDisplayUsername()).isEqualTo("NewUsername");
        verify(developerRepository).save(developer);
    }

    @Test
    void updateProfile_updatingTitle_shouldPersistNewTitle() {
        when(developerRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer));
        when(developerRepository.save(any(Developer.class))).thenReturn(developer);

        final ProfileUpdateRequest request = new ProfileUpdateRequest(null, "Lead Engineer", null);
        authService.updateProfile("dev@example.com", request);

        assertThat(developer.getTitle()).isEqualTo("Lead Engineer");
    }

    @Test
    void updateProfile_updatingDiscordLink_shouldPersistNewLink() {
        when(developerRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer));
        when(developerRepository.save(any(Developer.class))).thenReturn(developer);

        final ProfileUpdateRequest request = new ProfileUpdateRequest(null, null, "https://discord.gg/newlink");
        authService.updateProfile("dev@example.com", request);

        assertThat(developer.getDiscordLink()).isEqualTo("https://discord.gg/newlink");
    }

    @Test
    void updateProfile_withNullFields_shouldNotOverwriteExistingValues() {
        developer.setUsername("ExistingUsername");
        developer.setTitle("Existing Title");
        when(developerRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer));
        when(developerRepository.save(any(Developer.class))).thenReturn(developer);

        final ProfileUpdateRequest request = new ProfileUpdateRequest(null, null, null);
        authService.updateProfile("dev@example.com", request);

        assertThat(developer.getDisplayUsername()).isEqualTo("ExistingUsername");
        assertThat(developer.getTitle()).isEqualTo("Existing Title");
    }

    // --- generateInviteToken tests ---

    @Test
    void generateInviteToken_shouldReturnInviteResponseWithLink() {
        when(jwtTokenProvider.generateInviteToken("invited@example.com"))
                .thenReturn("invite-jwt-token");
        doNothing().when(emailService).sendInviteEmail(anyString(), anyString());

        final InviteRequest request = new InviteRequest("invited@example.com");
        final InviteResponse response = authService.generateInviteToken(request, "http://localhost:5173");

        assertThat(response.inviteLink()).contains("invite-jwt-token");
        assertThat(response.inviteLink()).contains("http://localhost:5173");
        verify(emailService).sendInviteEmail(eq("invited@example.com"), anyString());
    }

    @Test
    void generateInviteToken_linkShouldContainRegisterPath() {
        when(jwtTokenProvider.generateInviteToken("dev@example.com")).thenReturn("invite-token");
        doNothing().when(emailService).sendInviteEmail(anyString(), anyString());

        final InviteRequest request = new InviteRequest("dev@example.com");
        final InviteResponse response = authService.generateInviteToken(request, "http://app.example.com");

        assertThat(response.inviteLink()).contains("/register?token=");
    }
}
