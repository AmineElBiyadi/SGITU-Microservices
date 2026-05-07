package com.agileflow.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${g10.notifications.enabled:true}")
    private boolean enabled;

    @Value("${g10.notifications.email-url}")
    private String emailUrl;

    @Value("${g10.notifications.bearer-token:}")
    private String bearerToken;

    @Value("${g10.public-base-url}")
    private String publicBaseUrl;

    @Value("${g10.email.log-tokens:false}")
    private boolean logTokens;

    public void sendVerificationEmail(Long userId, String email, String rawToken) {
        String link = publicBaseUrl + "/auth/verify-email?token=" + rawToken;
        if (logTokens) {
            log.info("DEV email verification link for {}: {}", email, link);
        }
        sendEmail(
                "VERIFY_EMAIL",
                userId,
                email,
                Map.of(
                        "verificationLink", link,
                        "sourceType", "ACCOUNT",
                        "sourceId", userId
                )
        );
    }

    public void sendPasswordResetEmail(Long userId, String email, String rawToken) {
        String resetLink = publicBaseUrl + "/auth/reset-password?token=" + rawToken;
        if (logTokens) {
            log.info("DEV password reset token for {}: {}", email, rawToken);
        }
        sendEmail(
                "RESET_PASSWORD",
                userId,
                email,
                Map.of(
                        "resetLink", resetLink,
                        "sourceType", "ACCOUNT",
                        "sourceId", userId
                )
        );
    }

    private void sendEmail(String eventType, Long userId, String email, Map<String, Object> metadata) {
        if (!enabled) {
            log.info("Notification G5 desactivee. eventType={}, email={}, metadata={}", eventType, email, metadata);
            return;
        }

        WebClient.RequestBodySpec request = webClientBuilder.build()
                .post()
                .uri(emailUrl)
                .contentType(MediaType.APPLICATION_JSON);

        if (bearerToken != null && !bearerToken.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        request.bodyValue(buildEmailPayload(eventType, userId, email, metadata))
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(3))
                .subscribe(
                        response -> log.info("Notification G5 envoyee. eventType={}, email={}", eventType, email),
                        error -> log.warn("Notification G5 non envoyee. eventType={}, email={}, cause={}",
                                eventType, email, error.getMessage())
                );
    }

    Map<String, Object> buildEmailPayload(String eventType,
                                          Long userId,
                                          String email,
                                          Map<String, Object> metadata) {
        return Map.of(
                "notificationId", "auth-" + UUID.randomUUID(),
                "sourceService", "AUTH",
                "eventType", eventType,
                "channel", "EMAIL",
                "priority", "NORMAL",
                "recipient", Map.of(
                        "userId", userId.toString(),
                        "email", email
                ),
                "metadata", metadata
        );
    }
}
