package com.timesheet.controller.mapper;

import com.timesheet.dto.DeveloperResponse;
import com.timesheet.dto.LoginResponse;
import com.timesheet.model.Developer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeveloperMapper {

    @Mapping(source = "displayUsername", target = "username")
    @Mapping(target = "role", expression = "java(developer.getRole().name())")
    DeveloperResponse toDeveloperResponse(Developer developer);

    List<DeveloperResponse> toDeveloperResponseList(List<Developer> developers);

    @Mapping(source = "developer.id", target = "id")
    @Mapping(source = "developer.email", target = "email")
    @Mapping(source = "developer.displayUsername", target = "username")
    @Mapping(source = "developer.title", target = "title")
    @Mapping(source = "developer.discordLink", target = "discordLink")
    @Mapping(source = "developer.discordAvatarUrl", target = "discordAvatarUrl")
    @Mapping(target = "role", expression = "java(developer.getRole().name())")
    @Mapping(source = "developer.createdAt", target = "createdAt")
    @Mapping(source = "developer.updatedAt", target = "updatedAt")
    LoginResponse toLoginResponse(Developer developer, String token);
}
