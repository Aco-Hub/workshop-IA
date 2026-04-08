package com.timesheet.config;

import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import com.timesheet.repository.DeveloperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "albin@test.com";
    private static final String ADMIN_PASSWORD = "Vernhes";
    private static final String ADMIN_USERNAME = "Albin";
    private static final String ADMIN_TITLE = "Administrator";
    private static final String ADMIN_DISCORD = "https://discord.com/users/378853461464186890";

    private final DeveloperRepository developerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(final String... args) {
        developerRepository.findByEmail(ADMIN_EMAIL).ifPresentOrElse(
                admin -> {
                    // Verify the password hash is valid BCrypt
                    if (!passwordEncoder.matches(ADMIN_PASSWORD, admin.getPassword())) {
                        log.info("Updating admin password hash...");
                        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
                        developerRepository.save(admin);
                        log.info("Admin password hash updated.");
                    } else {
                        log.info("Admin account verified: {}", ADMIN_EMAIL);
                    }
                },
                () -> {
                    log.info("Creating admin account: {}", ADMIN_EMAIL);
                    final Developer admin = Developer.builder()
                            .email(ADMIN_EMAIL)
                            .password(passwordEncoder.encode(ADMIN_PASSWORD))
                            .username(ADMIN_USERNAME)
                            .title(ADMIN_TITLE)
                            .discordLink(ADMIN_DISCORD)
                            .role(DeveloperRole.ADMIN)
                            .build();
                    developerRepository.save(admin);
                    log.info("Admin account created: {}", ADMIN_EMAIL);
                }
        );
    }
}
