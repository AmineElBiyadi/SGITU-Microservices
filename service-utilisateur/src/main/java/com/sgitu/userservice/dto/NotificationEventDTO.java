package com.sgitu.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Payload format for notifications sent to Group 5 via Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDTO {

    /**
     * Type of event (e.g., WELCOME, ACCOUNT_DEACTIVATED).
     */
    private String eventType;

    /**
     * Metadata containing user details (userId, email, username, timestamp).
     */
    private Map<String, String> metadata;
}
