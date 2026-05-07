package com.sgitu.servicegestionincidents.messaging.consumer;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import com.sgitu.servicegestionincidents.messaging.event.IncidentDetecteEvent;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.model.entity.Localisation;
import com.sgitu.servicegestionincidents.model.enums.NiveauGravite;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.model.enums.TypeIncident;
import com.sgitu.servicegestionincidents.repository.IncidentRepository;
import com.sgitu.servicegestionincidents.service.NotificationService;
import com.sgitu.servicegestionincidents.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuiviVehiculeConsumer {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final ReferenceGenerator referenceGenerator;

    @KafkaListener(topics = MessagingConstants.SUIVI_VEHICULE_TOPIC, groupId = MessagingConstants.GROUP_ID)
    public void recevoirIncidentVehicule(IncidentDetecteEvent event) {
        log.info("Incident détecté par IoT - Type: {}, Véhicule: {}", event.getType(), event.getVehiculeId());

        try {
            Localisation localisation = Localisation.builder()
                    .latitude(event.getLatitude())
                    .longitude(event.getLongitude())
                    .ligneTransport(event.getLigneTransport())
                    .build();

            Incident incident = Incident.builder()
                    .reference(referenceGenerator.generate())
                    .source("IOT")
                    .type(TypeIncident.valueOf(event.getType()))
                    .statut(StatutIncident.NOUVEAU)
                    .gravite(NiveauGravite.valueOf(event.getGravite()))
                    .vehiculeId(event.getVehiculeId())
                    .declarantId(0L) // 0L = Système automatique IoT
                    .description(event.getDescription())
                    .localisation(localisation)
                    .dateSignalement(LocalDateTime.now())
                    .dateIncident(event.getDateDetection())
                    .build();

            Incident saved = incidentRepository.save(incident);
            log.info("Incident créé automatiquement: {}", saved.getReference());

            notificationService.envoyerAlerteIoT(saved);

        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement IoT: {}", e.getMessage(), e);
        }
    }
}
