package com.timesheet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Override
    public void sendInviteEmail(final String toEmail, final String inviteLink) {
        try {
            final SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("You've been invited to Timesheet");
            message.setText("""
                    You have been invited to join the Timesheet application.

                    Click the link below to complete your registration:
                    %s

                    This link will expire in 7 days.

                    If you did not expect this invitation, please ignore this email.
                    """.formatted(inviteLink));

            mailSender.send(message);
            log.info("Invite email sent to: {}", toEmail);
        } catch (MailException e) {
            // Gracefully handle SMTP not configured or other mail errors
            log.warn("Failed to send invite email to {}: {}. The invite link is: {}",
                    toEmail, e.getMessage(), inviteLink);
        } catch (Exception e) {
            log.warn("Unexpected error sending invite email to {}: {}", toEmail, e.getMessage());
        }
    }
}
