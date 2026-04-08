package com.timesheet.service;

import com.timesheet.model.Developer;
import com.timesheet.repository.DeveloperRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordOAuthService {

    private static final String DISCORD_TOKEN_URL = "https://discord.com/api/oauth2/token";
    private static final String DISCORD_USER_URL = "https://discord.com/api/users/@me";
    private static final String DISCORD_CDN_URL = "https://cdn.discordapp.com/avatars/%s/%s.png";

    private final RestTemplate restTemplate;
    private final DeveloperRepository developerRepository;

    @Value("${discord.client-id:}")
    private String clientId;

    @Value("${discord.client-secret:}")
    private String clientSecret;

    @Value("${discord.redirect-uri:http://localhost:8080/api/auth/discord/callback}")
    private String redirectUri;

    public String getAuthorizationUrl() {
        return "https://discord.com/api/oauth2/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=identify";
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Developer handleCallback(final String code, final String developerEmail) {
        if (!isConfigured()) {
            throw new IllegalStateException("Discord OAuth is not configured");
        }

        // Exchange code for access token
        final String accessToken = exchangeCodeForToken(code);

        // Fetch Discord user info
        final Map<String, Object> discordUser = fetchDiscordUser(accessToken);

        final String discordId = (String) discordUser.get("id");
        final String avatarHash = (String) discordUser.get("avatar");
        final String discordUsername = (String) discordUser.get("username");

        final String discordLink = "https://discord.com/users/" + discordId;
        final String avatarUrl = (avatarHash != null)
                ? DISCORD_CDN_URL.formatted(discordId, avatarHash)
                : null;

        final Developer developer = developerRepository.findByEmail(developerEmail)
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + developerEmail));

        developer.setDiscordLink(discordLink);
        developer.setDiscordAvatarUrl(avatarUrl);

        return developerRepository.save(developer);
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    @SuppressWarnings("unchecked")
    private String exchangeCodeForToken(final String code) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", redirectUri);

        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        final ResponseEntity<Map> response = restTemplate.postForEntity(DISCORD_TOKEN_URL, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to exchange Discord code for token");
        }

        return (String) response.getBody().get("access_token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchDiscordUser(final String accessToken) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        final HttpEntity<Void> request = new HttpEntity<>(headers);

        final ResponseEntity<Map> response = restTemplate.exchange(
                DISCORD_USER_URL, HttpMethod.GET, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to fetch Discord user info");
        }

        return response.getBody();
    }
}
