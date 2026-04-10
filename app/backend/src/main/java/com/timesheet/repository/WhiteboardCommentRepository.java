package com.timesheet.repository;

import com.timesheet.model.WhiteboardComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhiteboardCommentRepository extends JpaRepository<WhiteboardComment, Long> {

    List<WhiteboardComment> findByRoomIdOrderByCreatedAtAsc(Long roomId);
}
