package com.timesheet.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String name
) {}
