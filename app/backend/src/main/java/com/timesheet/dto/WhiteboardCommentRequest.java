package com.timesheet.dto;

import jakarta.validation.constraints.NotBlank;

public record WhiteboardCommentRequest(
        @NotBlank String text,
        Long parentId
) {}
