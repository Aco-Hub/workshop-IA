package com.timesheet.service;

public interface EmailService {

    void sendInviteEmail(String toEmail, String inviteLink);
}
