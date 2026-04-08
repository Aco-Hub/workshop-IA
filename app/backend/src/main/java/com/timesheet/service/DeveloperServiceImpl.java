package com.timesheet.service;

import com.timesheet.dto.InviteRequest;
import com.timesheet.dto.InviteResponse;
import com.timesheet.model.Developer;
import com.timesheet.repository.DeveloperRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeveloperServiceImpl implements DeveloperService {

    private final DeveloperRepository developerRepository;
    private final AuthService authService;

    @Override
    @Transactional(readOnly = true)
    public List<Developer> getAllDevelopers() {
        return developerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Developer getDeveloperById(final Long id) {
        return developerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Developer not found: " + id));
    }

    @Override
    @Transactional
    public void deleteDeveloper(final Long id) {
        if (!developerRepository.existsById(id)) {
            throw new EntityNotFoundException("Developer not found: " + id);
        }
        developerRepository.deleteById(id);
    }

    @Override
    public InviteResponse inviteDeveloper(final InviteRequest request, final String baseUrl) {
        return authService.generateInviteToken(request, baseUrl);
    }
}
