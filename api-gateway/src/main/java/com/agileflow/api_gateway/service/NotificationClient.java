package com.agileflow.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${g10.notifications.enabled:true}")
    private boolean enabled;

    @Value("${g10.notifications.email-url}")
    private String emailUrl;

    @Value("${g10.public-base-url}")
    private String publicBaseUrl;

    @Value("${g10.email.log-tokens:false}")
    private boolean logTokens;

    public void sendVerificationEmail(String email, String rawToken) {
        String link = publicBaseUrl + "/auth/verify-email?token=" + rawToken;
        if (logTokens) {
            log.info("DEV email verification link for {}: {}", email, link);
        }
        sendEmail(
                "VERIFY_EMAIL",
                email,
                "Verification de votre email SGITU",
                Map.of(
                        "verificationLink", link,
                        "token", rawToken
                )
        );
    }

    public void sendPasswordResetEmail(String email, String rawToken) {
        if (logTokens) {
            log.info("DEV password reset token for {}: {}", email, rawToken);
        }
        sendEmail(
                "RESET_PASSWORD",
                email,
                "Reinitialisation du mot de passe SGITU",
                Map.of(
                        "resetToken", rawToken,
                        "resetEndpoint", publicBaseUrl + "/auth/reset-password"
                )
        );
    }

    private void sendEmail(String type, String to, String subject, Map<String, String> data) {
        if (!enabled) {
            log.info("Notification G5 desactivee. type={}, to={}, data={}", type, to, data);
            return;
        }

        Map<String, Object> payload = Map.of(
                "type", type,
                "channel", "EMAIL",
                "to", to,
                "subject", subject,
                "data", data
        );

        webClientBuilder.build()
                .post()
                .uri(emailUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(3))
                .subscribe(
                        response -> log.info("Notification G5 envoyee. type={}, to={}", type, to),
                        error -> log.warn("Notification G5 non envoyee. type={}, to={}, cause={}",
                                type, to, error.getMessage())
                );
    }
}
