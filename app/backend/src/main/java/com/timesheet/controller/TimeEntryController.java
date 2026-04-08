package com.timesheet.controller;

import com.timesheet.controller.mapper.TimeEntryMapper;
import com.timesheet.dto.RecurringTimeEntryRequest;
import com.timesheet.dto.TimeEntryRequest;
import com.timesheet.dto.TimeEntryResponse;
import com.timesheet.service.TimeEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;
    private final TimeEntryMapper timeEntryMapper;

    @GetMapping
    public ResponseEntity<List<TimeEntryResponse>> getAllTimeEntries(
            @RequestParam(required = false) final Long developerId,
            @RequestParam(required = false) final Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endDate) {
        return ResponseEntity.ok(
                timeEntryMapper.toTimeEntryResponseList(
                        timeEntryService.getAllTimeEntries(developerId, projectId, startDate, endDate)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeEntryResponse> getTimeEntryById(@PathVariable final Long id) {
        return ResponseEntity.ok(timeEntryMapper.toTimeEntryResponse(timeEntryService.getTimeEntryById(id)));
    }

    @PostMapping
    public ResponseEntity<TimeEntryResponse> createTimeEntry(
            @Valid @RequestBody final TimeEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeEntryMapper.toTimeEntryResponse(timeEntryService.createTimeEntry(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimeEntryResponse> updateTimeEntry(
            @PathVariable final Long id,
            @Valid @RequestBody final TimeEntryRequest request) {
        return ResponseEntity.ok(timeEntryMapper.toTimeEntryResponse(timeEntryService.updateTimeEntry(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeEntry(@PathVariable final Long id) {
        timeEntryService.deleteTimeEntry(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recurring")
    public ResponseEntity<List<TimeEntryResponse>> createRecurringEntries(
            @Valid @RequestBody final RecurringTimeEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeEntryMapper.toTimeEntryResponseList(
                        timeEntryService.createRecurringEntries(request)));
    }

    @DeleteMapping("/recurring/{groupId}")
    public ResponseEntity<Void> deleteByRecurrenceGroup(@PathVariable final UUID groupId) {
        timeEntryService.deleteByRecurrenceGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/recurring/{groupId}")
    public ResponseEntity<List<TimeEntryResponse>> updateByRecurrenceGroup(
            @PathVariable final UUID groupId,
            @Valid @RequestBody final TimeEntryRequest request) {
        return ResponseEntity.ok(
                timeEntryMapper.toTimeEntryResponseList(
                        timeEntryService.updateByRecurrenceGroup(groupId, request)));
    }
}
