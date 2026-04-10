package com.timesheet.controller;

import com.timesheet.controller.mapper.WhiteboardMapper;
import com.timesheet.dto.CursorPositionMessage;
import com.timesheet.dto.WhiteboardElementRequest;
import com.timesheet.dto.WhiteboardElementResponse;
import com.timesheet.dto.WhiteboardEventMessage;
import com.timesheet.model.Developer;
import com.timesheet.model.WhiteboardElement;
import com.timesheet.service.WhiteboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WhiteboardWebSocketController {

    private final SimpMessagingTemplate _messagingTemplate;
    private final WhiteboardService _whiteboardService;
    private final WhiteboardMapper _whiteboardMapper;

    @MessageMapping("/whiteboard/{roomId}/cursor")
    public void handleCursorMove(
            @DestinationVariable final Long roomId,
            @Payload final CursorPositionMessage message) {
        _messagingTemplate.convertAndSend("/topic/whiteboard/" + roomId + "/cursors", message);
    }

    @MessageMapping("/whiteboard/{roomId}/element.create")
    public void handleElementCreate(
            @DestinationVariable final Long roomId,
            @Payload final WhiteboardElementRequest request,
            final Principal principal) {
        final Long userId = extractUserId(principal);
        final WhiteboardElement element = _whiteboardService.createElement(roomId, request, userId);
        final WhiteboardEventMessage event = new WhiteboardEventMessage("CREATE", _whiteboardMapper.toElementResponse(element));
        _messagingTemplate.convertAndSend("/topic/whiteboard/" + roomId + "/elements", event);
    }

    @MessageMapping("/whiteboard/{roomId}/element.delete")
    public void handleElementDelete(
            @DestinationVariable final Long roomId,
            @Payload final Long elementId) {
        _whiteboardService.deleteElement(elementId);
        final WhiteboardElementResponse stub = new WhiteboardElementResponse(elementId, null, null, null, null, null, null, null);
        final WhiteboardEventMessage event = new WhiteboardEventMessage("DELETE", stub);
        _messagingTemplate.convertAndSend("/topic/whiteboard/" + roomId + "/elements", event);
    }

    private Long extractUserId(final Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            final Object p = auth.getPrincipal();
            if (p instanceof Developer dev) {
                return dev.getId();
            }
        }
        throw new IllegalStateException("Cannot extract user from principal");
    }
}
