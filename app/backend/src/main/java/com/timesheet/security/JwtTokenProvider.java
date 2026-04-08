package com.timesheet.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long inviteExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret-key}") final String secretKeyBase64,
            @Value("${app.jwt.expiration}") final long expirationMs,
            @Value("${app.jwt.invite-expiration}") final long inviteExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyBase64));
        this.expirationMs = expirationMs;
        this.inviteExpirationMs = inviteExpirationMs;
    }

    public String generateToken(final String email, final String clientIp) {
        final Date now = new Date();
        final Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("ip", clientIp)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String generateInviteToken(final String email) {
        final Date now = new Date();
        final Date expiry = new Date(now.getTime() + inviteExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("type", "invite")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(final String token, final String clientIp) {
        try {
            final Claims claims = parseClaims(token);
            final String tokenIp = claims.get("ip", String.class);
            if (tokenIp != null && !tokenIp.equals(clientIp)) {
                log.warn("JWT IP mismatch: token IP={}, request IP={}", tokenIp, clientIp);
                return false;
            }
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateInviteToken(final String token) {
        try {
            final Claims claims = parseClaims(token);
            final String type = claims.get("type", String.class);
            return "invite".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid invite token: {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(final String token) {
        return parseClaims(token).getSubject();
    }

    public Claims extractAllClaims(final String token) {
        return parseClaims(token);
    }

    private Claims parseClaims(final String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
