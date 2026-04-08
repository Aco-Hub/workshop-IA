package com.timesheet.controller;

import com.timesheet.controller.mapper.DeveloperMapper;
import com.timesheet.dto.*;
import com.timesheet.model.Developer;
import com.timesheet.service.AuthService;
import com.timesheet.service.DiscordOAuthService;
import com.timesheet.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final DiscordOAuthService discordOAuthService;
    private final DeveloperMapper developerMapper;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody final LoginRequest request,
            final HttpServletRequest httpRequest) {
        final String clientIp = IpUtils.extractClientIp(httpRequest);
        final AuthResult result = authService.login(request, clientIp);
        return ResponseEntity.ok(developerMapper.toLoginResponse(result.developer(), result.token()));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(
            @Valid @RequestBody final RegisterRequest request,
            final HttpServletRequest httpRequest) {
        final String clientIp = IpUtils.extractClientIp(httpRequest);
        final AuthResult result = authService.register(request, clientIp);
        return ResponseEntity.ok(developerMapper.toLoginResponse(result.developer(), result.token()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<DeveloperResponse> getCurrentUser(
            @AuthenticationPrincipal final Developer developer) {
        return ResponseEntity.ok(developerMapper.toDeveloperResponse(developer));
    }

    @PutMapping("/profile")
    public ResponseEntity<DeveloperResponse> updateProfile(
            @AuthenticationPrincipal final Developer developer,
            @RequestBody final ProfileUpdateRequest request) {
        final Developer updated = authService.updateProfile(developer.getEmail(), request);
        return ResponseEntity.ok(developerMapper.toDeveloperResponse(updated));
    }

    @GetMapping("/discord/connect")
    public ResponseEntity<String> discordConnect() {
        if (!discordOAuthService.isConfigured()) {
            return ResponseEntity.badRequest().body("Discord OAuth is not configured");
        }
        return ResponseEntity.ok(discordOAuthService.getAuthorizationUrl());
    }

    @GetMapping("/discord/callback")
    public ResponseEntity<DeveloperResponse> discordCallback(
            @RequestParam final String code,
            @AuthenticationPrincipal final Developer developer) {
        final Developer updated = discordOAuthService.handleCallback(code, developer.getEmail());
        return ResponseEntity.ok(developerMapper.toDeveloperResponse(updated));
    }
}
