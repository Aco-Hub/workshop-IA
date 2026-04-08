package com.timesheet.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecurringTimeEntryRequest(
        @NotNull Long developerId,
        Long projectId,
        @NotNull String type,
        String description,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotNull String frequency,
        LocalDate endDate,
        Integer count
) {}
