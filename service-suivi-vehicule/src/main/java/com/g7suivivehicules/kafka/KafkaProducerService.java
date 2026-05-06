package com.g7suivivehicules.kafka;

import com.g7suivivehicules.dto.G4AnomalieTerrainDTO;
import com.g7suivivehicules.dto.G9IncidentEventDTO;
import com.g7suivivehicules.entity.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.anomalie}")
    private String topicAnomalieG4;

    @Value("${kafka.topic.incident}")
    private String topicIncidentG9;

    public void publierAlerte(Alert alert) {
        // Envoi à G4 (Anomalies Terrain)
        publierVersG4(alert);

        // Envoi à G9 (Incidents) si HAUTE ou CRITIQUE
        if (alert.getSeverite() == Alert.Severite.HAUTE || alert.getSeverite() == Alert.Severite.CRITIQUE) {
            publierVersG9(alert);
        }
    }

    private void publierVersG4(Alert alert) {
        String typeG4;
        switch (alert.getTypeAlert()) {
            case RETARD_HORAIRE:
                typeG4 = "RETARD";
                break;
            case DEVIATION_ITINERAIRE:
                typeG4 = "DEVIATION";
                break;
            case TEMPERATURE_CRITIQUE:
            case CARBURANT_CRITIQUE:
            case FREINAGE_BRUSQUE:
            case IMMOBILISATION:
                typeG4 = "PANNE";
                break;
            case VITESSE_EXCESSIVE:
            default:
                typeG4 = "PANNE"; // Default fallback if needed
                break;
        }

        G4AnomalieTerrainDTO dtoG4 = G4AnomalieTerrainDTO.builder()
                .vehiculeId(alert.getVehiculeId().toString())
                .type(typeG4)
                .details(alert.getMessage())
                .timestamp(alert.getTimestampDebut().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        kafkaTemplate.send(topicAnomalieG4, dtoG4);
        log.info("[KafkaProducer] Anomalie G4 publiée : {}", dtoG4);
    }

    private void publierVersG9(Alert alert) {
        String typeG9;
        switch (alert.getTypeAlert()) {
            case TEMPERATURE_CRITIQUE:
            case CARBURANT_CRITIQUE:
            case FREINAGE_BRUSQUE:
            case IMMOBILISATION:
                typeG9 = "PANNE_VEHICULE";
                break;
            case RETARD_HORAIRE:
                typeG9 = "RETARD";
                break;
            case DEVIATION_ITINERAIRE:
                typeG9 = "AUTRE"; // Or whatever fits best in G9
                break;
            case VITESSE_EXCESSIVE:
            default:
                typeG9 = "SECURITE";
                break;
        }

        String graviteG9;
        switch (alert.getSeverite()) {
            case MOYENNE:
                graviteG9 = "MOYEN";
                break;
            case HAUTE:
                graviteG9 = "ELEVE";
                break;
            case CRITIQUE:
                graviteG9 = "CRITIQUE";
                break;
            case FAIBLE:
            default:
                graviteG9 = "FAIBLE";
                break;
        }

        G9IncidentEventDTO dtoG9 = G9IncidentEventDTO.builder()
                .type(typeG9)
                .gravite(graviteG9)
                .description(alert.getMessage())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .vehiculeId(alert.getVehiculeId().toString()) // Remarque : String utilisé ici au lieu de Long pour passer l'UUID
                .dateDetection(alert.getTimestampDebut().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        kafkaTemplate.send(topicIncidentG9, dtoG9);
        log.info("[KafkaProducer] Incident G9 publié : {}", dtoG9);
    }
}
