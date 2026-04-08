package com.timesheet.dto;

public record ProfileUpdateRequest(
        String username,
        String title,
        String discordLink
) {}
