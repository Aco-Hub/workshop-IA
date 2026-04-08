package com.timesheet.dto;

import java.time.LocalDateTime;

public record TimeEntryResponse(
        Long id,
        Long developerId,
        String developerUsername,
        Long projectId,
        String projectName,
        String type,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String recurrenceGroupId,
        String recurrenceRule,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
