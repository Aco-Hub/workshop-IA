package com.timesheet.config;

import com.timesheet.model.Developer;
import com.timesheet.repository.DeveloperRepository;
import com.timesheet.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider _jwtTokenProvider;
    private final DeveloperRepository _developerRepository;

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            final String token = extractToken(accessor);
            if (token != null) {
                final String clientIp = extractClientIp(accessor);
                if (_jwtTokenProvider.validateToken(token, clientIp)) {
                    final String email = _jwtTokenProvider.extractEmail(token);
                    final Developer developer = _developerRepository.findByEmail(email).orElse(null);
                    if (developer != null) {
                        final UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(developer, null, developer.getAuthorities());
                        accessor.setUser(auth);
                    }
                }
            }
        }

        return message;
    }

    private String extractToken(final StompHeaderAccessor accessor) {
        final List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            final String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractClientIp(final StompHeaderAccessor accessor) {
        final Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            final Object ip = sessionAttributes.get("REMOTE_ADDR");
            if (ip != null) {
                return ip.toString();
            }
        }
        return "0.0.0.0";
    }
}
