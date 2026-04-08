package com.timesheet;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptHashTest {

    @Test
    void verifyAdminPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Vernhes");
        System.out.println("BCrypt hash for 'Vernhes': " + hash);
        assertTrue(encoder.matches("Vernhes", hash));
    }
}
