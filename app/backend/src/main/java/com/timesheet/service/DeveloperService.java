package com.timesheet.service;

import com.timesheet.dto.InviteRequest;
import com.timesheet.dto.InviteResponse;
import com.timesheet.model.Developer;

import java.util.List;

public interface DeveloperService {

    List<Developer> getAllDevelopers();

    Developer getDeveloperById(Long id);

    void deleteDeveloper(Long id);

    InviteResponse inviteDeveloper(InviteRequest request, String baseUrl);
}
