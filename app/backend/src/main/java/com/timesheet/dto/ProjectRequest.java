package com.timesheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectRequest(
        @NotBlank String name,
        @NotNull String type,
        String repoUrl,
        Long clientId
) {}
