package com.agileflow.api_gateway.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationClientTest {

    @Test
    void buildEmailPayloadMatchesG5ContractForVerificationEmail() {
        NotificationClient notificationClient = new NotificationClient(WebClient.builder());

        Map<String, Object> payload = notificationClient.buildEmailPayload(
                "VERIFY_EMAIL",
                10L,
                "user@sgitu.ma",
                Map.of(
                        "verificationLink", "http://localhost:8080/auth/verify-email?token=abc",
                        "sourceType", "ACCOUNT",
                        "sourceId", 10L
                )
        );

        assertThat(payload)
                .containsEntry("sourceService", "AUTH")
                .containsEntry("eventType", "VERIFY_EMAIL")
                .containsEntry("channel", "EMAIL")
                .containsEntry("priority", "NORMAL")
                .containsKeys("notificationId", "recipient", "metadata");

        assertThat(payload.get("notificationId").toString()).startsWith("auth-");

        assertThat((Map<String, Object>) payload.get("recipient"))
                .containsEntry("userId", "10")
                .containsEntry("email", "user@sgitu.ma");

        assertThat((Map<String, Object>) payload.get("metadata"))
                .containsEntry("verificationLink", "http://localhost:8080/auth/verify-email?token=abc")
                .containsEntry("sourceType", "ACCOUNT")
                .containsEntry("sourceId", 10L)
                .doesNotContainKey("eventType");
    }

    @Test
    void buildEmailPayloadMatchesG5ContractForPasswordReset() {
        NotificationClient notificationClient = new NotificationClient(WebClient.builder());

        Map<String, Object> payload = notificationClient.buildEmailPayload(
                "RESET_PASSWORD",
                10L,
                "user@sgitu.ma",
                Map.of(
                        "resetLink", "http://localhost:8080/auth/reset-password?token=xyz",
                        "sourceType", "ACCOUNT",
                        "sourceId", 10L
                )
        );

        assertThat(payload).containsEntry("eventType", "RESET_PASSWORD");
        assertThat((Map<String, Object>) payload.get("metadata"))
                .containsEntry("resetLink", "http://localhost:8080/auth/reset-password?token=xyz")
                .doesNotContainKey("eventType");
    }
}
