package com.sgitu.userservice.service;

import com.sgitu.userservice.dto.NotificationEventDTO;
import com.sgitu.userservice.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending user-related notifications to Kafka (Group 5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private static final String TOPIC = "user-events";

    /**
     * Publishes a notification event to Kafka asynchronously.
     *
     * @param eventType Type of the event (WELCOME, ACCOUNT_DEACTIVATED, etc.)
     * @param user The user concernced by the event
     */
    @Async
    public void sendNotification(String eventType, User user) {
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
            log.error("Error sending Kafka notification: {}", e.getMessage());
        }
    }
}
