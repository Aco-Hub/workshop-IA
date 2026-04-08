package com.timesheet.repository;

import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DeveloperRepositoryTest {

    @Autowired
    private DeveloperRepository developerRepository;

    @Test
    void findByEmail_whenExists_shouldReturnDeveloper() {
        Developer developer = Developer.builder()
                .email("test@example.com")
                .password("encoded-password")
                .username("TestUser")
                .role(DeveloperRole.STANDARD)
                .build();
        developerRepository.save(developer);

        Optional<Developer> found = developerRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getDisplayUsername()).isEqualTo("TestUser");
    }

    @Test
    void findByEmail_whenNotExists_shouldReturnEmpty() {
        Optional<Developer> found = developerRepository.findByEmail("notfound@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_whenExists_shouldReturnTrue() {
        Developer developer = Developer.builder()
                .email("exists@example.com")
                .password("encoded-password")
                .role(DeveloperRole.STANDARD)
                .build();
        developerRepository.save(developer);

        assertThat(developerRepository.existsByEmail("exists@example.com")).isTrue();
    }

    @Test
    void existsByEmail_whenNotExists_shouldReturnFalse() {
        assertThat(developerRepository.existsByEmail("missing@example.com")).isFalse();
    }
}
