package com.timesheet.service;

import com.timesheet.dto.WhiteboardCommentRequest;
import com.timesheet.dto.WhiteboardElementRequest;
import com.timesheet.dto.WhiteboardRoomRequest;
import com.timesheet.model.WhiteboardComment;
import com.timesheet.model.WhiteboardElement;
import com.timesheet.model.WhiteboardRoom;

import java.util.List;

public interface WhiteboardService {

    List<WhiteboardRoom> getAllRooms();

    WhiteboardRoom getRoomById(Long id);

    WhiteboardRoom createRoom(WhiteboardRoomRequest request, Long creatorId);

    void deleteRoom(Long id);

    List<WhiteboardElement> getElementsByRoomId(Long roomId);

    WhiteboardElement createElement(Long roomId, WhiteboardElementRequest request, Long creatorId);

    WhiteboardElement updateElement(Long elementId, WhiteboardElementRequest request);

    void deleteElement(Long elementId);

    void clearRoom(Long roomId);

    List<WhiteboardComment> getCommentsByRoomId(Long roomId);

    WhiteboardComment addComment(Long roomId, WhiteboardCommentRequest request, Long developerId);

    void deleteComment(Long commentId);
}
