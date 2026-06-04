package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.G5NotificationRequest;
import com.g7suivivehicules.dto.G9IncidentEventDTO;
import com.g7suivivehicules.entity.AlertStatus;
import com.g7suivivehicules.entity.PendingAlert;
import com.g7suivivehicules.entity.PendingIncident;
import com.g7suivivehicules.repository.PendingAlertRepository;
import com.g7suivivehicules.repository.PendingIncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduler de retry automatique pour les alertes et incidents stockés localement
 * suite à une indisponibilité des services G5 (Notification) ou G9 (Incidents).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private static final int MAX_TENTATIVES = 10;

    private final PendingAlertRepository pendingAlertRepository;
    private final PendingIncidentRepository pendingIncidentRepository;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${g5.notification.url}")
    private String g5Url;

    @Value("${kafka.topic.incident}")
    private String topicIncidentG9;

    @Scheduled(fixedDelay = 30_000)
    public void retryPendingAlerts() {
        List<PendingAlert> pending = pendingAlertRepository.findByStatus(AlertStatus.PENDING);
        if (pending.isEmpty()) return;

        log.info("[RetryScheduler][G5] {} alerte(s) PENDING à renvoyer vers G5...", pending.size());

        for (PendingAlert alert : pending) {
            if (alert.getTentatives() >= MAX_TENTATIVES) {
                log.error("[RetryScheduler][G5] Alerte {} abandonnée après {} tentatives → statut FAILED",
                        alert.getId(), alert.getTentatives());
                alert.setStatus(AlertStatus.FAILED);
                pendingAlertRepository.save(alert);
                continue;
            }

            try {
                G5NotificationRequest request = G5NotificationRequest.builder()
                        .notificationId("retry-g7-" + UUID.randomUUID().toString().substring(0, 8))
                        .eventType(alert.getTypeAnomalie())
                        .priority(alert.getPriority())
                        .recipient(G5NotificationRequest.Recipient.builder()
                                .userId(alert.getVehiculeId().startsWith("conducteur-") ? alert.getVehiculeId() : "conducteur-" + alert.getVehiculeId())
                                .email("conducteur@sgitu.ma")
                                .deviceToken("token-retry-" + alert.getVehiculeId())
                                .build())
                        .build();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-User-Id", "system-g7-scheduler");
                headers.set("X-Roles", "ROLE_G7_SERVICE");
                HttpEntity<G5NotificationRequest> entity = new HttpEntity<>(request, headers);

                restTemplate.postForEntity(g5Url, entity, String.class);

                alert.setStatus(AlertStatus.SENT);
                alert.setLastAttemptAt(LocalDateTime.now());
                pendingAlertRepository.save(alert);
                log.info("[RetryScheduler][G5] ✅ Alerte {} envoyée avec succès → SENT", alert.getId());

            } catch (Exception e) {
                alert.setTentatives(alert.getTentatives() + 1);
                alert.setLastAttemptAt(LocalDateTime.now());
                pendingAlertRepository.save(alert);
                log.warn("[RetryScheduler][G5] ❌ G5 toujours indisponible pour alerte {} (tentative {}/{}): {}",
                        alert.getId(), alert.getTentatives(), MAX_TENTATIVES, e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void retryPendingIncidents() {
        List<PendingIncident> pending = pendingIncidentRepository.findByStatus(AlertStatus.PENDING);
        if (pending.isEmpty()) return;

        log.info("[RetryScheduler][G9] {} incident(s) PENDING à renvoyer vers G9 via Kafka...", pending.size());

        for (PendingIncident incident : pending) {
            if (incident.getTentatives() >= MAX_TENTATIVES) {
                log.error("[RetryScheduler][G9] Incident {} abandonné après {} tentatives → statut FAILED",
                        incident.getId(), incident.getTentatives());
                incident.setStatus(AlertStatus.FAILED);
                pendingIncidentRepository.save(incident);
                continue;
            }

            try {
                G9IncidentEventDTO dto = G9IncidentEventDTO.builder()
                        .type(incident.getTypeIncident())
                        .gravite(incident.getGravite())
                        .description("[RETRY] " + incident.getDescription())
                        .latitude(incident.getLatitude())
                        .longitude(incident.getLongitude())
                        .vehiculeId(incident.getVehiculeId())
                        .dateDetection(incident.getDateDetection())
                        .build();

                kafkaTemplate.send(topicIncidentG9, incident.getVehiculeId(), dto).get();

                incident.setStatus(AlertStatus.SENT);
                incident.setLastAttemptAt(LocalDateTime.now());
                pendingIncidentRepository.save(incident);
                log.info("[RetryScheduler][G9] ✅ Incident {} envoyé avec succès → SENT", incident.getId());

            } catch (Exception e) {
                incident.setTentatives(incident.getTentatives() + 1);
                incident.setLastAttemptAt(LocalDateTime.now());
                pendingIncidentRepository.save(incident);
                log.warn("[RetryScheduler][G9] ❌ Kafka/G9 toujours indisponible (tentative {}/{}): {}",
                        incident.getId(), incident.getTentatives(), MAX_TENTATIVES, e.getMessage());
            }
        }
    }
}
