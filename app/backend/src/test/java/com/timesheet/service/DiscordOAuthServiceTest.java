package com.timesheet.service;

import com.timesheet.repository.DeveloperRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DiscordOAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DeveloperRepository developerRepository;

    @InjectMocks
    private DiscordOAuthService discordOAuthService;

    @Test
    void isConfigured_whenClientIdAndSecretEmpty_shouldReturnFalse() {
        // Default mock: fields are null/empty
        assertThat(discordOAuthService.isConfigured()).isFalse();
    }

    @Test
    void handleCallback_whenNotConfigured_shouldThrow() {
        assertThatThrownBy(() -> discordOAuthService.handleCallback("code", "test@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void getAuthorizationUrl_shouldContainOAuthParams() {
        // Even without config, url structure should be valid
        String url = discordOAuthService.getAuthorizationUrl();
        assertThat(url).contains("discord.com/api/oauth2/authorize");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("scope=identify");
    }
}
