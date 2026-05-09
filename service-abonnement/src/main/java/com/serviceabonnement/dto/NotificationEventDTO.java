package com.serviceabonnement.dto;

import com.serviceabonnement.dto.enums.NotificationChannel;
import com.serviceabonnement.dto.enums.NotificationEventType;
import com.serviceabonnement.dto.enums.NotificationPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEventDTO {
    private String notificationId;
    private String sourceService;
    private NotificationEventType eventType;
    private NotificationChannel channel;
    private NotificationPriority priority;
    private RecipientDTO recipient;
    private Map<String, Object> metadata;
}
