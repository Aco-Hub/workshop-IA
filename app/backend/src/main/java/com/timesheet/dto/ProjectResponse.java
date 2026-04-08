package com.timesheet.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String type,
        String repoUrl,
        ClientResponse client,
        List<DeveloperResponse> assignedDevelopers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
