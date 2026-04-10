package com.timesheet.dto;

import java.time.LocalDateTime;

public record WhiteboardCommentResponse(
        Long id,
        DeveloperResponse developer,
        String text,
        Long parentId,
        LocalDateTime createdAt
) {}
