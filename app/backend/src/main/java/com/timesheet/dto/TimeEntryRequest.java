package com.timesheet.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TimeEntryRequest(
        @NotNull Long developerId,
        Long projectId,
        @NotNull String type,
        String description,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
) {}
