package com.timesheet.dto;

import com.timesheet.model.Developer;

public record AuthResult(Developer developer, String token) {}
