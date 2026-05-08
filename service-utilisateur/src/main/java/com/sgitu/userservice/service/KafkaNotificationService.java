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
            NotificationEventDTO event = NotificationEventDTO.builder()
                    .eventType(eventType)
                    .metadata(buildMetadata(user))
                    .build();

            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("Sending Kafka notification: {} for user {}", eventType, user.getEmail());
            kafkaTemplate.send(TOPIC, jsonPayload);
        } catch (Exception e) {
            log.error("Error sending Kafka notification: {}", e.getMessage());
        }
    }

    private Map<String, String> buildMetadata(User user) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", String.valueOf(user.getId()));
        metadata.put("email", user.getEmail());
        
        String username = "User";
        if (user.getProfile() != null) {
            username = user.getProfile().getFirstName() + " " + user.getProfile().getLastName();
        }
        metadata.put("username", username.trim());
        
        metadata.put("timestamp", ZonedDateTime.now(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT));
                
        return metadata;
    }
}
