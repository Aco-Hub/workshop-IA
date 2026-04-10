package com.timesheet.repository;

import com.timesheet.model.WhiteboardRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhiteboardRoomRepository extends JpaRepository<WhiteboardRoom, Long> {

    List<WhiteboardRoom> findAllByOrderByCreatedAtDesc();
}
