package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.G5NotificationRequest;
import com.g7suivivehicules.entity.Alert;
import com.g7suivivehicules.entity.AlertStatus;
import com.g7suivivehicules.entity.PendingAlert;
import com.g7suivivehicules.repository.PendingAlertRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class G5NotificationService {

    private final RestTemplate restTemplate;
    private final PendingAlertRepository pendingAlertRepository;

    @Value("${g5.notification.url}")
    private String g5Url;

    @CircuitBreaker(name = "g5NotificationService", fallbackMethod = "notifierConducteurFallback")
    @Retry(name = "g5NotificationService")
    @TimeLimiter(name = "g5NotificationService")
    public CompletableFuture<Void> notifierConducteur(Alert alert) {
        try {
            String priority = determinerPriorite(alert.getTypeAlert());
            String message = genererMessageConducteur(alert);

            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication() != null 
                    ? SecurityContextHolder.getContext().getAuthentication().getName() 
                    : "conducteur@sgitu.ma";

            Map<String, String> metadata = new HashMap<>();
            metadata.put("vehiculeId", alert.getVehiculeId().toString());
            metadata.put("typeAnomalie", alert.getTypeAlert().name());
            metadata.put("valeur", alert.getValeur() != null ? alert.getValeur().toString() : "N/A");
            metadata.put("seuil", alert.getSeuil() != null ? alert.getSeuil().toString() : "N/A");
            metadata.put("message", message);
            metadata.put("timestamp", alert.getTimestampDebut().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            G5NotificationRequest request = G5NotificationRequest.builder()
                    .notificationId("uuid-g7-" + UUID.randomUUID().toString().substring(0, 8))
                    .eventType("VEHICULE_ALERTE_CONDUCTEUR")
                    .priority(priority)
                    .recipient(G5NotificationRequest.Recipient.builder()
                            .userId("conducteur-" + alert.getVehiculeId())
                            .email(currentUserEmail)
                            .deviceToken("token-device-conducteur-" + alert.getVehiculeId())
                            .build())
                    .metadata(metadata)
                    .build();

            HttpHeaders headers = buildG5Headers();
            HttpEntity<G5NotificationRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(g5Url, entity, String.class);

            log.info("[G5Notification] Notification PUSH envoyée pour véhicule {} (Alerte: {})",
                    alert.getVehiculeId(), alert.getTypeAlert());

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("[G5Notification] Échec de l'envoi vers G5 : {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Void> notifierConducteurFallback(Alert alert, Exception e) {
        log.warn("[G5][FALLBACK] G5 injoignable pour véhicule {} (raison: {}) — stockage local PENDING",
                alert.getVehiculeId(), e.getMessage());

        String message = genererMessageConducteur(alert);
        String priority = determinerPriorite(alert.getTypeAlert());

        PendingAlert pending = PendingAlert.builder()
                .vehiculeId(alert.getVehiculeId().toString())
                .typeAnomalie(alert.getTypeAlert().name())
                .message("[ALERTE CONDUCTEUR] " + message)
                .priority(priority)
                .payloadJson("{\"eventType\":\"VEHICULE_ALERTE_CONDUCTEUR\",\"vehiculeId\":\"" + alert.getVehiculeId() + "\"}") 
                .createdAt(LocalDateTime.now())
                .status(AlertStatus.PENDING)
                .tentatives(0)
                .build();

        pendingAlertRepository.save(pending);
        log.info("[G5][FALLBACK] Alerte sauvegardée en DB avec id={} (sera renvoyée automatiquement)", pending.getId());

        return CompletableFuture.completedFuture(null);
    }

    private String determinerPriorite(Alert.TypeAlert type) {
        switch (type) {
            case TEMPERATURE_CRITIQUE:
            case CARBURANT_CRITIQUE:
            case VITESSE_EXCESSIVE:
                return "HIGH";
            default:
                return "NORMAL";
        }
    }

    private String genererMessageConducteur(Alert alert) {
        switch (alert.getTypeAlert()) {
            case TEMPERATURE_CRITIQUE:
                return "Température moteur critique, arrêtez le véhicule";
            case CARBURANT_CRITIQUE:
                return "Niveau carburant très bas, faites le plein";
            case VITESSE_EXCESSIVE:
                return "Vitesse excessive, réduisez la vitesse";
            case FREINAGE_BRUSQUE:
                return "Freinage brusque détecté, soyez prudent";
            case IMMOBILISATION:
                return "Arrêt anormal détecté, vérifiez la situation";
            case DEVIATION_ITINERAIRE:
                return "Déviation d'itinéraire détectée";
            default:
                return "Alerte de sécurité détectée sur votre véhicule";
        }
    }

    @CircuitBreaker(name = "g5NotificationService", fallbackMethod = "notifierLogAdminFallback")
    @Retry(name = "g5NotificationService")
    @TimeLimiter(name = "g5NotificationService")
    public CompletableFuture<Void> notifierLogAdmin(String logLevel, String message) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("logLevel", logLevel);
        metadata.put("serviceName", "G7_SUIVI_VEHICULES");
        metadata.put("message", message);
        metadata.put("timestamp", java.time.LocalDateTime.now().toString());
        metadata.put("source", "G7");

        G5NotificationRequest request = G5NotificationRequest.builder()
                .notificationId("uuid-g7-" + UUID.randomUUID().toString().substring(0, 8))
                .eventType("LOG_ALERT_ADMIN")
                .channel("EMAIL")
                .priority(determinePriorityFromLogLevel(logLevel))
                .recipient(G5NotificationRequest.Recipient.builder()
                        .userId("admin")
                        .email("admin@sgitu.ma")
                        .deviceToken(null)
                        .build())
                .metadata(metadata)
                .build();

        HttpHeaders headers = buildG5Headers();
        HttpEntity<G5NotificationRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity(g5Url, entity, String.class);

        log.info("[G5Notification] Notification LOG envoyée à l'admin - Level: {}, Message: {}", logLevel, message);
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> notifierLogAdminFallback(String logLevel, String message, Exception e) {
        log.warn("[G5][FALLBACK] G5 injoignable pour log admin [{}] — stockage local PENDING", logLevel);

        PendingAlert pending = PendingAlert.builder()
                .vehiculeId("ADMIN")
                .typeAnomalie("LOG_" + logLevel.toUpperCase())
                .message("[LOG ADMIN][" + logLevel + "] " + message)
                .priority(determinePriorityFromLogLevel(logLevel))
                .payloadJson("{\"eventType\":\"LOG_ALERT_ADMIN\",\"logLevel\":\"" + logLevel + "\"}") 
                .createdAt(LocalDateTime.now())
                .status(AlertStatus.PENDING)
                .tentatives(0)
                .build();

        pendingAlertRepository.save(pending);
        log.info("[G5][FALLBACK] Log admin sauvegardé en DB avec id={}", pending.getId());

        return CompletableFuture.completedFuture(null);
    }

    @CircuitBreaker(name = "g5NotificationService", fallbackMethod = "notifierVehiculeEnregistreFallback")
    @Retry(name = "g5NotificationService")
    @TimeLimiter(name = "g5NotificationService")
    public CompletableFuture<Void> notifierVehiculeEnregistre(com.g7suivivehicules.dto.VehiculeResponse vehicule) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("vehiculeId", vehicule.getId().toString());
        metadata.put("immatriculation", vehicule.getImmatriculation());
        metadata.put("type", vehicule.getType().name());
        metadata.put("message", "Votre véhicule a été enregistré avec succès dans le système SGITU.");
        metadata.put("timestamp", java.time.LocalDateTime.now().toString());

        G5NotificationRequest request = G5NotificationRequest.builder()
                .notificationId("uuid-g7-" + UUID.randomUUID().toString().substring(0, 8))
                .eventType("VEHICULE_ENREGISTRE")
                .priority("NORMAL")
                .recipient(G5NotificationRequest.Recipient.builder()
                        .userId("conducteur-" + vehicule.getConducteurId())
                        .email("conducteur@sgitu.ma") 
                        .deviceToken(null)
                        .build())
                .metadata(metadata)
                .build();

        HttpHeaders headers = buildG5Headers();
        HttpEntity<G5NotificationRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity(g5Url, entity, String.class);

        log.info("[G5Notification] Notification d'enregistrement envoyée pour : {}", vehicule.getImmatriculation());
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> notifierVehiculeEnregistreFallback(com.g7suivivehicules.dto.VehiculeResponse vehicule, Exception e) {
        log.warn("[G5][FALLBACK] G5 injoignable pour enregistrement véhicule {} — stockage local PENDING",
                vehicule.getImmatriculation());

        PendingAlert pending = PendingAlert.builder()
                .vehiculeId(vehicule.getId().toString())
                .typeAnomalie("VEHICULE_ENREGISTRE")
                .message("Notification d'enregistrement non envoyée pour : " + vehicule.getImmatriculation())
                .priority("NORMAL")
                .payloadJson("{\"eventType\":\"VEHICULE_ENREGISTRE\",\"immatriculation\":\"" + vehicule.getImmatriculation() + "\"}") 
                .createdAt(LocalDateTime.now())
                .status(AlertStatus.PENDING)
                .tentatives(0)
                .build();

        pendingAlertRepository.save(pending);
        log.info("[G5][FALLBACK] Notification enregistrement sauvegardée en DB avec id={}", pending.getId());

        return CompletableFuture.completedFuture(null);
    }

    private HttpHeaders buildG5Headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        org.springframework.web.context.request.ServletRequestAttributes attributes = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
            if (request != null) {
                String jwt = request.getHeader("Authorization");
                if (jwt != null) {
                    headers.set("Authorization", jwt);
                }
                String email = request.getHeader("X-User-Email");
                if (email != null) {
                    headers.set("X-User-Email", email);
                }
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    headers.set("X-User-Id", userId);
                }
                String roles = request.getHeader("X-Roles");
                if (roles != null) {
                    headers.set("X-Roles", roles);
                }
            }
        }
        
        if (!headers.containsKey("X-User-Id") && !headers.containsKey("X-User-Email")) {
            headers.set("X-User-Id", "system-g7");
            headers.set("X-Roles", "ROLE_G7_SERVICE");
        }
        
        return headers;
    }

    private String determinePriorityFromLogLevel(String logLevel) {
        return switch (logLevel.toUpperCase()) {
            case "ERROR", "FATAL" -> "HIGH";
            case "WARN" -> "NORMAL";
            default -> "LOW";
        };
    }
}
