package com.timesheet.dto;

public record CursorPositionMessage(
        Long userId,
        String username,
        String color,
        double x,
        double y
) {}
