package com.timesheet.dto;

import java.time.LocalDateTime;

public record WhiteboardElementResponse(
        Long id,
        String type,
        String data,
        String color,
        Integer strokeWidth,
        Long createdById,
        Integer zIndex,
        LocalDateTime createdAt
) {}
