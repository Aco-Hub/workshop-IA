package com.timesheet.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // A valid base64-encoded secret key for testing (at least 256 bits)
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0xMjM0NTY3ODk=";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, 86400000L, 604800000L);
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtTokenProvider.generateToken("test@example.com", "127.0.0.1");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void validateToken_withValidTokenAndMatchingIp_shouldReturnTrue() {
        String token = jwtTokenProvider.generateToken("test@example.com", "127.0.0.1");
        assertThat(jwtTokenProvider.validateToken(token, "127.0.0.1")).isTrue();
    }

    @Test
    void validateToken_withMismatchedIp_shouldReturnFalse() {
        String token = jwtTokenProvider.generateToken("test@example.com", "127.0.0.1");
        assertThat(jwtTokenProvider.validateToken(token, "192.168.1.1")).isFalse();
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        String email = "user@example.com";
        String token = jwtTokenProvider.generateToken(email, "127.0.0.1");
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void generateInviteToken_shouldBeValidatedAsInvite() {
        String token = jwtTokenProvider.generateInviteToken("invite@example.com");
        assertThat(jwtTokenProvider.validateInviteToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldNotValidateInviteTokenWithIpCheck() {
        String inviteToken = jwtTokenProvider.generateInviteToken("invite@example.com");
        // Invite tokens have no 'ip' claim, so IP validation should pass (null ip in token)
        assertThat(jwtTokenProvider.validateToken(inviteToken, "127.0.0.1")).isTrue();
    }

    @Test
    void extractEmail_fromInviteToken_shouldWork() {
        String email = "invite@example.com";
        String token = jwtTokenProvider.generateInviteToken(email);
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo(email);
    }
}
