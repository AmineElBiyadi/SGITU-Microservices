package com.sgitu.userservice.service;

import com.sgitu.userservice.dto.UserStatusEventDTO;
import com.sgitu.userservice.entity.EventStatus;
import com.sgitu.userservice.entity.FailedEvent;
import com.sgitu.userservice.repository.FailedEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Publishes user status-change events to external consumers.
 *
 * Resilience4j Circuit Breaker intercepts Kafka publication.
 * If Kafka is down (or simulated DOWN), the fallback method persists the event to PostgreSQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FailedEventRepository failedEventRepository;
    private final ChaosMonkeyService chaosMonkeyService;

    @Value("${events.user-status.topic:g8-user-events}")
    private String topic;

    /**
     * Sends a single status-change event wrapped in a list (batch of one).
     *
     * @param userId numeric user ID
     * @param action "active" or "inactive"
     */
    @Async
    @CircuitBreaker(name = "kafkaAnalytics", fallbackMethod = "publishFallback")
    public void publish(Long userId, String action) {
        chaosMonkeyService.checkKafka();

        if (topic == null || topic.isBlank()) {
            log.debug("events.user-status.topic not configured — skipping event publication.");
            return;
        }

        UserStatusEventDTO event = buildEvent(userId, action);
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, payload);
            log.info("Published user-status event to Kafka topic {}: {}", topic, payload);
        } catch (Exception ex) {
            throw new RuntimeException("Kafka publish failed", ex);
        }
    }

    /**
     * Fallback method called by Resilience4j when publish throws an exception.
     * Persists the event in the outbox table for scheduled retry.
     */
    public void publishFallback(Long userId, String action, Throwable t) {
        log.error("Kafka DOWN (analytics fallback). Saving failed user-status event in DB. Error: {}", t.getMessage());
        try {
            UserStatusEventDTO event = buildEvent(userId, action);
            String payload = objectMapper.writeValueAsString(event);

            FailedEvent failedEvent = FailedEvent.builder()
                    .topic(topic != null ? topic : "g8-user-events")
                    .payload(payload)
                    .eventType("USER_STATUS")
                    .status(EventStatus.PENDING)
                    .build();

            failedEventRepository.save(failedEvent);
            log.info("Saved failed user-status event to database outbox.");
        } catch (Exception ex) {
            log.error("Critical: failed to persist failed user-status event!", ex);
        }
    }

    /**
     * Kept for backward compatibility, delegates to publish.
     */
    @Async
    public void publishBatch(List<UserStatusEventDTO> events) {
        for (UserStatusEventDTO event : events) {
            try {
                publish(Long.parseLong(event.getUserId()), event.getAction());
            } catch (Exception e) {
                log.error("Error in publishBatch: {}", e.getMessage());
            }
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
