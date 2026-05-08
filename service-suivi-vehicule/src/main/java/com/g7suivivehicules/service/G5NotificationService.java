package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.G5NotificationRequest;
import com.g7suivivehicules.entity.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class G5NotificationService {

    private final RestTemplate restTemplate;

    @Value("${g5.notification.url}")
    private String g5Url;

    public void notifierConducteur(Alert alert) {
        try {
            String priority = determinerPriorite(alert.getTypeAlert());
            String message = genererMessageConducteur(alert);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("vehiculeId", alert.getVehiculeId().toString());
            metadata.put("typeAnomalie", alert.getTypeAlert().name());
            metadata.put("valeur", alert.getValeur() != null ? alert.getValeur().toString() : "N/A");
            metadata.put("seuil", alert.getSeuil() != null ? alert.getSeuil().toString() : "N/A");
            metadata.put("message", message);
            metadata.put("timestamp", alert.getTimestampDebut().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            G5NotificationRequest request = G5NotificationRequest.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .eventType("VEHICULE_ALERTE_CONDUCTEUR")
                    .priority(priority)
                    .recipient(G5NotificationRequest.Recipient.builder()
                            .userId("conducteur-" + alert.getVehiculeId())
                            .deviceToken("token-device-conducteur-" + alert.getVehiculeId()) // Mock token
                            .build())
                    .metadata(metadata)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // On pourrait ajouter le JWT ici si on l'avait dans le contexte
            // headers.setBearerAuth(jwt);

            HttpEntity<G5NotificationRequest> entity = new HttpEntity<>(request, headers);
            
            restTemplate.postForEntity(g5Url, entity, String.class);
            
            log.info("[G5Notification] Notification PUSH envoyée pour véhicule {} (Alerte: {})", 
                    alert.getVehiculeId(), alert.getTypeAlert());

        } catch (Exception e) {
            log.error("[G5Notification] Échec de l'envoi vers G5 : {}", e.getMessage());
        }
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
            case TEMPERATURE_CRITIQUE: return "Température moteur critique, arrêtez le véhicule";
            case CARBURANT_CRITIQUE: return "Niveau carburant très bas, faites le plein";
            case VITESSE_EXCESSIVE: return "Vitesse excessive, réduisez la vitesse";
            case FREINAGE_BRUSQUE: return "Freinage brusque détecté, soyez prudent";
            case IMMOBILISATION: return "Arrêt anormal détecté, vérifiez la situation";
            case DEVIATION_ITINERAIRE: return "Déviation d'itinéraire détectée";
            default: return "Alerte de sécurité détectée sur votre véhicule";
        }
    }
}
