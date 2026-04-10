package com.timesheet.service;

import com.timesheet.dto.WhiteboardCommentRequest;
import com.timesheet.dto.WhiteboardElementRequest;
import com.timesheet.dto.WhiteboardRoomRequest;
import com.timesheet.model.*;
import com.timesheet.repository.DeveloperRepository;
import com.timesheet.repository.WhiteboardCommentRepository;
import com.timesheet.repository.WhiteboardElementRepository;
import com.timesheet.repository.WhiteboardRoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WhiteboardServiceImpl implements WhiteboardService {

    private final WhiteboardRoomRepository _roomRepository;
    private final WhiteboardElementRepository _elementRepository;
    private final WhiteboardCommentRepository _commentRepository;
    private final DeveloperRepository _developerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WhiteboardRoom> getAllRooms() {
        return _roomRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public WhiteboardRoom getRoomById(final Long id) {
        return _roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Whiteboard room not found: " + id));
    }

    @Override
    @Transactional
    public WhiteboardRoom createRoom(final WhiteboardRoomRequest request, final Long creatorId) {
        final Developer creator = _developerRepository.findById(creatorId)
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + creatorId));

        final WhiteboardRoom room = WhiteboardRoom.builder()
                .name(request.name())
                .createdBy(creator)
                .build();

        return _roomRepository.save(room);
    }

    @Override
    @Transactional
    public void deleteRoom(final Long id) {
        if (!_roomRepository.existsById(id)) {
            throw new EntityNotFoundException("Whiteboard room not found: " + id);
        }
        _roomRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhiteboardElement> getElementsByRoomId(final Long roomId) {
        return _elementRepository.findByRoomIdOrderByZIndexAsc(roomId);
    }

    @Override
    @Transactional
    public WhiteboardElement createElement(final Long roomId, final WhiteboardElementRequest request, final Long creatorId) {
        final WhiteboardRoom room = _roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Whiteboard room not found: " + roomId));
        final Developer creator = _developerRepository.findById(creatorId)
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + creatorId));

        final WhiteboardElement element = WhiteboardElement.builder()
                .room(room)
                .type(ElementType.valueOf(request.type()))
                .data(request.data())
                .color(request.color())
                .strokeWidth(request.strokeWidth())
                .createdBy(creator)
                .zIndex(request.zIndex() != null ? request.zIndex() : 0)
                .build();

        return _elementRepository.save(element);
    }

    @Override
    @Transactional
    public WhiteboardElement updateElement(final Long elementId, final WhiteboardElementRequest request) {
        final WhiteboardElement element = _elementRepository.findById(elementId)
                .orElseThrow(() -> new EntityNotFoundException("Whiteboard element not found: " + elementId));

        element.setType(ElementType.valueOf(request.type()));
        element.setData(request.data());
        element.setColor(request.color());
        element.setStrokeWidth(request.strokeWidth());
        if (request.zIndex() != null) {
            element.setZIndex(request.zIndex());
        }

        return _elementRepository.save(element);
    }

    @Override
    @Transactional
    public void deleteElement(final Long elementId) {
        if (!_elementRepository.existsById(elementId)) {
            throw new EntityNotFoundException("Whiteboard element not found: " + elementId);
        }
        _elementRepository.deleteById(elementId);
    }

    @Override
    @Transactional
    public void clearRoom(final Long roomId) {
        _elementRepository.deleteByRoomId(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhiteboardComment> getCommentsByRoomId(final Long roomId) {
        return _commentRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }

    @Override
    @Transactional
    public WhiteboardComment addComment(final Long roomId, final WhiteboardCommentRequest request, final Long developerId) {
        final WhiteboardRoom room = _roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Whiteboard room not found: " + roomId));
        final Developer developer = _developerRepository.findById(developerId)
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + developerId));

        final WhiteboardComment.WhiteboardCommentBuilder builder = WhiteboardComment.builder()
                .room(room)
                .developer(developer)
                .text(request.text());

        if (request.parentId() != null) {
            final WhiteboardComment parent = _commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent comment not found: " + request.parentId()));
            builder.parent(parent);
        }

        return _commentRepository.save(builder.build());
    }

    @Override
    @Transactional
    public void deleteComment(final Long commentId) {
        if (!_commentRepository.existsById(commentId)) {
            throw new EntityNotFoundException("Whiteboard comment not found: " + commentId);
        }
        _commentRepository.deleteById(commentId);
    }
}
