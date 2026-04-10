package com.timesheet.repository;

import com.timesheet.model.WhiteboardElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WhiteboardElementRepository extends JpaRepository<WhiteboardElement, Long> {

    @Query("SELECT e FROM WhiteboardElement e WHERE e.room.id = :roomId ORDER BY e.zIndex ASC")
    List<WhiteboardElement> findByRoomIdOrderByZIndexAsc(@Param("roomId") Long roomId);

    void deleteByRoomId(Long roomId);
}
