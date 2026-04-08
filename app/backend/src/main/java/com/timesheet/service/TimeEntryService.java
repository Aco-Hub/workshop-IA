package com.timesheet.service;

import com.timesheet.dto.RecurringTimeEntryRequest;
import com.timesheet.dto.TimeEntryRequest;
import com.timesheet.model.TimeEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TimeEntryService {

    List<TimeEntry> getAllTimeEntries(Long developerId, Long projectId,
                                      LocalDateTime startDate, LocalDateTime endDate);

    TimeEntry getTimeEntryById(Long id);

    TimeEntry createTimeEntry(TimeEntryRequest request);

    TimeEntry updateTimeEntry(Long id, TimeEntryRequest request);

    void deleteTimeEntry(Long id);

    List<TimeEntry> createRecurringEntries(RecurringTimeEntryRequest request);

    void deleteByRecurrenceGroup(UUID groupId);

    List<TimeEntry> updateByRecurrenceGroup(UUID groupId, TimeEntryRequest request);
}
