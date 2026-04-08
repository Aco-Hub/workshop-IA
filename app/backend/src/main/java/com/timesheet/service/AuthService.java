package com.timesheet.service;

import com.timesheet.dto.*;
import com.timesheet.model.Developer;

public interface AuthService {

    AuthResult login(LoginRequest request, String clientIp);

    AuthResult register(RegisterRequest request, String clientIp);

    InviteResponse generateInviteToken(InviteRequest request, String baseUrl);

    Developer getCurrentDeveloper(String email);

    Developer updateProfile(String email, ProfileUpdateRequest request);
}
