package com.serviceabonnement.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serviceabonnement.dto.enums.NotificationChannel;
import com.serviceabonnement.dto.enums.NotificationEventType;
import com.serviceabonnement.dto.enums.NotificationPriority;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotificationEventSerializationTest {

    @Test
    public void testSerializationFormat() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", "Premium");
        metadata.put("montantPaye", 29.99);

        NotificationEventDTO event = NotificationEventDTO.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService("SUBSCRIPTION")
                .eventType(NotificationEventType.CONFIRMATION_SOUSCRIPTION)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.HIGH)
                .recipient(RecipientDTO.builder()
                        .userId("5")
                        .email("user@example.com")
                        .build())
                .metadata(metadata)
                .build();

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event);
        System.out.println(json);

        // Verify eventType is at root
        assertTrue(json.contains("\"eventType\" : \"CONFIRMATION_SOUSCRIPTION\""));
        // Verify eventType is NOT in metadata
        assertFalse(json.contains("\"metadata\" : {\n    \"eventType\""));
        // Verify sourceService
        assertTrue(json.contains("\"sourceService\" : \"SUBSCRIPTION\""));
    }
}
