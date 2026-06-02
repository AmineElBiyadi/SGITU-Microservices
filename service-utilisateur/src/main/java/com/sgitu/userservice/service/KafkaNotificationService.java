package com.sgitu.userservice.service;

import com.sgitu.userservice.dto.NotificationEventDTO;
import com.sgitu.userservice.entity.EventStatus;
import com.sgitu.userservice.entity.FailedEvent;
import com.sgitu.userservice.entity.User;
import com.sgitu.userservice.repository.FailedEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending user-related notifications to Kafka (Group 5).
 *
 * Resilience4j Circuit Breaker intercepts Kafka notification sending.
 * If Kafka is down (or simulated DOWN), the fallback method persists the event to PostgreSQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final FailedEventRepository failedEventRepository;
    private final ChaosMonkeyService chaosMonkeyService;
    
    private static final String TOPIC = "user-events";

    /**
     * Publishes a notification event to Kafka asynchronously.
     *
     * @param eventType Type of the event (WELCOME, ACCOUNT_DEACTIVATED, etc.)
     * @param user The user concerned by the event
     */
    @Async
    @CircuitBreaker(name = "kafkaNotification", fallbackMethod = "sendNotificationFallback")
    public void sendNotification(String eventType, User user) {
        chaosMonkeyService.checkKafka();

        try {
            String username = "User";
            if (user.getProfile() != null) {
                username = user.getProfile().getFirstName() + " " + user.getProfile().getLastName();
            }

            NotificationEventDTO event = NotificationEventDTO.builder()
                    .eventType(eventType)
                    .userId(String.valueOf(user.getId()))
                    .email(user.getEmail())
                    .username(username.trim())
                    .timestamp(ZonedDateTime.now(ZoneId.of("UTC"))
                            .format(DateTimeFormatter.ISO_INSTANT))
                    .build();

            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("Sending Kafka notification: {} for user {}", eventType, user.getEmail());
            kafkaTemplate.send(TOPIC, jsonPayload);
        } catch (Exception e) {
            throw new RuntimeException("Kafka notification failed", e);
        }
    }

    /**
     * Fallback method called by Resilience4j when sendNotification throws an exception.
     * Persists the event in the outbox table for scheduled retry.
     */
    public void sendNotificationFallback(String eventType, User user, Throwable t) {
        log.error("Kafka DOWN (notification fallback). Saving failed notification event in DB. Error: {}", t.getMessage());
        try {
            String username = "User";
            if (user.getProfile() != null) {
                username = user.getProfile().getFirstName() + " " + user.getProfile().getLastName();
            }

            NotificationEventDTO event = NotificationEventDTO.builder()
                    .eventType(eventType)
                    .userId(String.valueOf(user.getId()))
                    .email(user.getEmail())
                    .username(username.trim())
                    .timestamp(ZonedDateTime.now(ZoneId.of("UTC"))
                            .format(DateTimeFormatter.ISO_INSTANT))
                    .build();

            String jsonPayload = objectMapper.writeValueAsString(event);

            FailedEvent failedEvent = FailedEvent.builder()
                    .topic(TOPIC)
                    .payload(jsonPayload)
                    .eventType(eventType)
                    .status(EventStatus.PENDING)
                    .build();

            failedEventRepository.save(failedEvent);
            log.info("Saved failed WELCOME/deactivation event to database outbox.");
        } catch (Exception ex) {
            log.error("Critical: failed to persist failed event to database!", ex);
        }
    }

    /**
     * Sends email verification code to the user via Kafka.
     *
     * @param user The user who needs email verification
     * @param verificationCode 6-digit verification code
     */
    @Async
    public void sendVerificationEmail(User user, String verificationCode) {
        try {
            String username = "User";
            if (user.getProfile() != null) {
                username = user.getProfile().getFirstName() + " " + user.getProfile().getLastName();
            }

            NotificationEventDTO event = NotificationEventDTO.builder()
                    .eventType("EMAIL_VERIFICATION")
                    .userId(String.valueOf(user.getId()))
                    .email(user.getEmail())
                    .username(username.trim())
                    .verificationCode(verificationCode)
                    .timestamp(ZonedDateTime.now(ZoneId.of("UTC"))
                            .format(DateTimeFormatter.ISO_INSTANT))
                    .build();

            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("Sending verification code via Kafka to: {}", user.getEmail());
            kafkaTemplate.send(TOPIC, jsonPayload);
        } catch (Exception e) {
            log.error("Error sending verification email: {}", e.getMessage());
        }
    }
}
