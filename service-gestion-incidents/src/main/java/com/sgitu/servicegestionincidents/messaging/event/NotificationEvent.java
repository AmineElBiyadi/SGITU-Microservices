package com.sgitu.servicegestionincidents.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {

    private String notificationId;
    private String sourceService;
    private String eventType;
    private String channel;
    private String priority;
    private Recipient recipient;
    private Map<String, Object> metadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Recipient implements Serializable {
        private String userId;
        private String email;
        private String phone;
        private String deviceToken;
    }
}
