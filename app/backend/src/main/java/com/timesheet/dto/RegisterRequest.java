package com.timesheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String token,
        @NotBlank String username,
        @NotBlank @Size(min = 8) String password,
        String title,
        String discordLink
) {}
