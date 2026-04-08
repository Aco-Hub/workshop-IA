package com.timesheet.dto;

import java.time.LocalDateTime;

public record ClientResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
