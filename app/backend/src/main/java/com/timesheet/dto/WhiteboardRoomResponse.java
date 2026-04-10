package com.timesheet.dto;

import java.time.LocalDateTime;

public record WhiteboardRoomResponse(
        Long id,
        String name,
        DeveloperResponse createdBy,
        LocalDateTime createdAt
) {}
