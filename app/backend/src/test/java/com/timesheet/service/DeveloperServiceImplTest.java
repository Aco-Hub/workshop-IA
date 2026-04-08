package com.timesheet.service;

import com.timesheet.model.Developer;
import com.timesheet.model.DeveloperRole;
import com.timesheet.repository.DeveloperRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeveloperServiceImplTest {

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private DeveloperServiceImpl developerService;

    private Developer developer;

    @BeforeEach
    void setUp() {
        developer = Developer.builder()
                .id(1L)
                .email("dev@example.com")
                .password("encoded")
                .username("DevUser")
                .role(DeveloperRole.STANDARD)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllDevelopers_shouldReturnAll() {
        when(developerRepository.findAll()).thenReturn(List.of(developer));

        final List<Developer> result = developerService.getAllDevelopers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("dev@example.com");
    }

    @Test
    void getDeveloperById_whenExists_shouldReturn() {
        when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));

        final Developer result = developerService.getDeveloperById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getDeveloperById_whenNotExists_shouldThrow() {
        when(developerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> developerService.getDeveloperById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteDeveloper_whenExists_shouldDelete() {
        when(developerRepository.existsById(1L)).thenReturn(true);

        developerService.deleteDeveloper(1L);

        verify(developerRepository).deleteById(1L);
    }

    @Test
    void deleteDeveloper_whenNotExists_shouldThrow() {
        when(developerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> developerService.deleteDeveloper(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
