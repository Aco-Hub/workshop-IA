package com.timesheet.dto;

import java.time.LocalDateTime;

public record DeveloperResponse(
        Long id,
        String email,
        String username,
        String title,
        String discordLink,
        String discordAvatarUrl,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
