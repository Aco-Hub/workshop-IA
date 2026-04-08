package com.timesheet.service;

import com.timesheet.dto.*;
import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import com.timesheet.repository.DeveloperRepository;
import com.timesheet.security.JwtTokenProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final DeveloperRepository developerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public AuthResult login(final LoginRequest request, final String clientIp) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        final Developer developer = (Developer) authentication.getPrincipal();
        final String token = jwtTokenProvider.generateToken(developer.getEmail(), clientIp);

        return new AuthResult(developer, token);
    }

    @Override
    @Transactional
    public AuthResult register(final RegisterRequest request, final String clientIp) {
        if (!jwtTokenProvider.validateInviteToken(request.token())) {
            throw new IllegalArgumentException("Invalid or expired invite token");
        }

        final String email = jwtTokenProvider.extractEmail(request.token());

        if (developerRepository.existsByEmail(email)) {
            throw new IllegalStateException("Account already exists for email: " + email);
        }

        Developer developer = Developer.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .username(request.username())
                .title(request.title())
                .discordLink(request.discordLink())
                .role(DeveloperRole.STANDARD)
                .build();

        developer = developerRepository.save(developer);
        log.info("New developer registered: {}", email);

        final String token = jwtTokenProvider.generateToken(developer.getEmail(), clientIp);

        return new AuthResult(developer, token);
    }

    @Override
    @Transactional(readOnly = true)
    public InviteResponse generateInviteToken(final InviteRequest request, final String baseUrl) {
        final String inviteToken = jwtTokenProvider.generateInviteToken(request.email());
        final String inviteLink = baseUrl + "/register?token=" + inviteToken;

        emailService.sendInviteEmail(request.email(), inviteLink);

        return new InviteResponse(inviteLink, "Invite email sent to " + request.email());
    }

    @Override
    @Transactional(readOnly = true)
    public Developer getCurrentDeveloper(final String email) {
        return developerRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + email));
    }

    @Override
    @Transactional
    public Developer updateProfile(final String email, final ProfileUpdateRequest request) {
        final Developer developer = developerRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + email));

        if (request.username() != null) {
            developer.setUsername(request.username());
        }
        if (request.title() != null) {
            developer.setTitle(request.title());
        }
        if (request.discordLink() != null) {
            developer.setDiscordLink(request.discordLink());
        }

        return developerRepository.save(developer);
    }
}
