package com.timesheet.dto;

public record WhiteboardEventMessage(
        String action,
        WhiteboardElementResponse element
) {}
