package com.sgitu.userservice.service;

import com.sgitu.userservice.dto.UserStatusEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

/**
 * Publishes user status-change events to external consumers.
 *
 * Rules agreed with consuming group:
 *  - Fire only on meaningful state changes (create → active, deactivate → inactive, reactivate → active).
 *  - Send a JSON array so multiple events can be batched into one request.
 *  - Never re-send unchanged data (no periodic polling).
 *
 * The call is @Async so it never blocks the main request thread.
 * A failure to notify does NOT roll back the user operation; it is logged as a warning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final RestTemplate restTemplate;

    @Value("${events.user-status.url:}")
    private String eventUrl;

    /**
     * Sends a single status-change event wrapped in a list (batch of one).
     * For bulk operations, call publishBatch() directly.
     *
     * @param userId numeric user ID
     * @param action "active" or "inactive"
     */
    @Async
    public void publish(Long userId, String action) {
        publishBatch(List.of(buildEvent(userId, action)));
    }

    /**
     * Sends multiple status-change events in a single HTTP request.
     */
    @Async
    public void publishBatch(List<UserStatusEventDTO> events) {
        if (eventUrl == null || eventUrl.isBlank()) {
            log.debug("events.user-status.url not configured — skipping event publication.");
            return;
        }
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(eventUrl, events, Void.class);
            log.info("Published {} user-status event(s) → {} (HTTP {})",
                    events.size(), eventUrl, response.getStatusCode());
        } catch (Exception ex) {
            // Notification failure must not break the main flow
            log.warn("Failed to publish user-status events to {}: {}", eventUrl, ex.getMessage());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserStatusEventDTO buildEvent(Long userId, String action) {
        return UserStatusEventDTO.builder()
                .userId(String.valueOf(userId))
                .action(action)
                .timestamp(Instant.now())
                .build();
    }
}

