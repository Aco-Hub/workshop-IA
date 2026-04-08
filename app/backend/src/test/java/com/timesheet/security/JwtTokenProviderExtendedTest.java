package com.timesheet.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderExtendedTest {

    private JwtTokenProvider jwtTokenProvider;

    // A valid base64-encoded secret key for testing (at least 256 bits)
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0xMjM0NTY3ODk=";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, 86400000L, 604800000L);
    }

    @Test
    void generateToken_tokenContainsEmbeddedIpClaim() {
        String token = jwtTokenProvider.generateToken("user@example.com", "10.0.0.1");
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("ip", String.class)).isEqualTo("10.0.0.1");
    }

    @Test
    void validateToken_withSameIp_shouldReturnTrue() {
        String token = jwtTokenProvider.generateToken("user@example.com", "192.168.0.5");
        assertThat(jwtTokenProvider.validateToken(token, "192.168.0.5")).isTrue();
    }

    @Test
    void validateToken_withDifferentIp_shouldReturnFalse() {
        String token = jwtTokenProvider.generateToken("user@example.com", "192.168.0.5");
        assertThat(jwtTokenProvider.validateToken(token, "10.10.10.10")).isFalse();
    }

    @Test
    void validateToken_withExpiredToken_shouldReturnFalse() {
        // Create provider with 1ms expiration to immediately expire
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(TEST_SECRET, 1L, 604800000L);
        String token = shortLivedProvider.generateToken("user@example.com", "127.0.0.1");

        // Wait for token to expire
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(shortLivedProvider.validateToken(token, "127.0.0.1")).isFalse();
    }

    @Test
    void generateInviteToken_containsEmailAsSubject() {
        String email = "invited@example.com";
        String token = jwtTokenProvider.generateInviteToken(email);
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void generateInviteToken_containsInviteTypeClaim() {
        String token = jwtTokenProvider.generateInviteToken("user@example.com");
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("type", String.class)).isEqualTo("invite");
    }

    @Test
    void validateInviteToken_withValidInviteToken_shouldReturnTrue() {
        String token = jwtTokenProvider.generateInviteToken("user@example.com");
        assertThat(jwtTokenProvider.validateInviteToken(token)).isTrue();
    }

    @Test
    void validateInviteToken_withRegularAuthToken_shouldReturnFalse() {
        // A regular auth token does not have type=invite
        String authToken = jwtTokenProvider.generateToken("user@example.com", "127.0.0.1");
        assertThat(jwtTokenProvider.validateInviteToken(authToken)).isFalse();
    }

    @Test
    void validateInviteToken_withGarbageString_shouldReturnFalse() {
        assertThat(jwtTokenProvider.validateInviteToken("not-a-jwt-at-all")).isFalse();
    }

    @Test
    void validateToken_withGarbageString_shouldReturnFalse() {
        assertThat(jwtTokenProvider.validateToken("garbage", "127.0.0.1")).isFalse();
    }

    @Test
    void extractEmail_afterTokenGeneration_shouldReturnOriginalEmail() {
        String originalEmail = "precise@example.com";
        String token = jwtTokenProvider.generateToken(originalEmail, "127.0.0.1");
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo(originalEmail);
    }

    @Test
    void generateToken_differentCallsProduceDifferentTokens() {
        String token1 = jwtTokenProvider.generateToken("a@example.com", "127.0.0.1");
        String token2 = jwtTokenProvider.generateToken("b@example.com", "127.0.0.1");
        assertThat(token1).isNotEqualTo(token2);
    }
}
