package com.timesheet.repository;

import com.timesheet.model.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByDeveloperId(Long developerId);

    List<TimeEntry> findByProjectId(Long projectId);

    List<TimeEntry> findByDeveloperIdAndStartTimeBetween(Long developerId, LocalDateTime start, LocalDateTime end);

    List<TimeEntry> findByRecurrenceGroupId(UUID recurrenceGroupId);

    void deleteByRecurrenceGroupId(UUID recurrenceGroupId);

    @Query(value = """
            SELECT te.* FROM time_entries te
            WHERE (:developerId IS NULL OR te.developer_id = :developerId)
              AND (:projectId IS NULL OR te.project_id = :projectId)
              AND (CAST(:startDate AS TIMESTAMP) IS NULL OR te.start_time >= :startDate)
              AND (CAST(:endDate AS TIMESTAMP) IS NULL OR te.start_time <= :endDate)
            ORDER BY te.start_time DESC
            """, nativeQuery = true)
    List<TimeEntry> findWithFilters(
            @Param("developerId") Long developerId,
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
