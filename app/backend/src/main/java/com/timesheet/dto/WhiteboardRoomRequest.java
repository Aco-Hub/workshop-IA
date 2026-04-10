package com.timesheet.dto;

import jakarta.validation.constraints.NotBlank;

public record WhiteboardRoomRequest(
        @NotBlank String name
) {}
