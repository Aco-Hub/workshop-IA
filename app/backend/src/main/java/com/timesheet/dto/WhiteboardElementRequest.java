package com.timesheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WhiteboardElementRequest(
        @NotBlank String type,
        @NotBlank String data,
        @NotBlank String color,
        @NotNull Integer strokeWidth,
        Integer zIndex
) {}
