package com.timesheet.config;

import com.timesheet.repository.DeveloperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@RequiredArgsConstructor
public class UserDetailsServiceConfig {

    private final DeveloperRepository developerRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> developerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Developer not found: " + email));
    }
}
