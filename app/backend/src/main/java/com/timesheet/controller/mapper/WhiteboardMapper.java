package com.timesheet.controller.mapper;

import com.timesheet.dto.WhiteboardCommentResponse;
import com.timesheet.dto.WhiteboardElementResponse;
import com.timesheet.dto.WhiteboardRoomResponse;
import com.timesheet.model.WhiteboardComment;
import com.timesheet.model.WhiteboardElement;
import com.timesheet.model.WhiteboardRoom;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DeveloperMapper.class})
public interface WhiteboardMapper {

    WhiteboardRoomResponse toRoomResponse(WhiteboardRoom room);

    List<WhiteboardRoomResponse> toRoomResponseList(List<WhiteboardRoom> rooms);

    @Mapping(target = "type", expression = "java(element.getType().name())")
    @Mapping(source = "createdBy.id", target = "createdById")
    WhiteboardElementResponse toElementResponse(WhiteboardElement element);

    List<WhiteboardElementResponse> toElementResponseList(List<WhiteboardElement> elements);

    @Mapping(source = "parent.id", target = "parentId")
    WhiteboardCommentResponse toCommentResponse(WhiteboardComment comment);

    List<WhiteboardCommentResponse> toCommentResponseList(List<WhiteboardComment> comments);
}
