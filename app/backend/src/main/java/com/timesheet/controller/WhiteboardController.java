package com.timesheet.controller;

import com.timesheet.controller.mapper.WhiteboardMapper;
import com.timesheet.dto.*;
import com.timesheet.model.Developer;
import com.timesheet.service.WhiteboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/whiteboards")
@RequiredArgsConstructor
public class WhiteboardController {

    private final WhiteboardService _whiteboardService;
    private final WhiteboardMapper _whiteboardMapper;

    @GetMapping
    public ResponseEntity<List<WhiteboardRoomResponse>> getAllRooms() {
        return ResponseEntity.ok(_whiteboardMapper.toRoomResponseList(_whiteboardService.getAllRooms()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WhiteboardRoomResponse> getRoomById(@PathVariable final Long id) {
        return ResponseEntity.ok(_whiteboardMapper.toRoomResponse(_whiteboardService.getRoomById(id)));
    }

    @PostMapping
    public ResponseEntity<WhiteboardRoomResponse> createRoom(
            @Valid @RequestBody final WhiteboardRoomRequest request,
            @AuthenticationPrincipal final Developer developer) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(_whiteboardMapper.toRoomResponse(_whiteboardService.createRoom(request, developer.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable final Long id) {
        _whiteboardService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/elements")
    public ResponseEntity<List<WhiteboardElementResponse>> getElements(@PathVariable final Long id) {
        return ResponseEntity.ok(_whiteboardMapper.toElementResponseList(_whiteboardService.getElementsByRoomId(id)));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<WhiteboardCommentResponse>> getComments(@PathVariable final Long id) {
        return ResponseEntity.ok(_whiteboardMapper.toCommentResponseList(_whiteboardService.getCommentsByRoomId(id)));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<WhiteboardCommentResponse> addComment(
            @PathVariable final Long id,
            @Valid @RequestBody final WhiteboardCommentRequest request,
            @AuthenticationPrincipal final Developer developer) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(_whiteboardMapper.toCommentResponse(_whiteboardService.addComment(id, request, developer.getId())));
    }

    @DeleteMapping("/{roomId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable final Long roomId,
            @PathVariable final Long commentId) {
        _whiteboardService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
