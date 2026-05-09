package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.messaging.event.NotificationEvent;
import com.sgitu.servicegestionincidents.messaging.producer.NotificationProducer;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationProducer notificationProducer;

    private NotificationEvent.NotificationEventBuilder buildBaseEvent(Incident incident, String eventType, String channel, String priority) {
        return NotificationEvent.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService("G9_INCIDENTS")
                .eventType(eventType)
                .channel(channel)
                .priority(priority)
                .recipient(NotificationEvent.Recipient.builder()
                        .userId(incident.getDeclarantId() != null ? incident.getDeclarantId().toString() : "SYSTEM")
                        // TODO: Fetch from user service
                        .email("contact@sgitu.ma")
                        .phone("+212600000000")
                        .build());
    }

    @Override
    public void envoyerConfirmation(Incident incident) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("incidentId", incident.getId());
        metadata.put("reference", incident.getReference());
        metadata.put("type", incident.getType().name());
        metadata.put("dateSignalement", incident.getDateSignalement().toString());
        metadata.put("statut", incident.getStatut().name());
        metadata.put("lienSuivi", "https://sgitu.ma/suivi/" + incident.getReference());

        NotificationEvent event = buildBaseEvent(incident, "INCIDENT_CONFIRMATION", "EMAIL", "NORMAL")
                .metadata(metadata)
                .build();

        notificationProducer.envoyerNotification(event);
    }

    @Override
    public void envoyerChangementStatut(Incident incident, String ancienStatut) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reference", incident.getReference());
        metadata.put("ancienStatut", ancienStatut);
        metadata.put("nouveauStatut", incident.getStatut().name());
        metadata.put("dateChangement", java.time.LocalDateTime.now().toString());

        NotificationEvent event = buildBaseEvent(incident, "INCIDENT_CHANGEMENT_STATUT", "PUSH", "NORMAL")
                .metadata(metadata)
                .build();

        notificationProducer.envoyerNotification(event);
    }

    @Override
    public void envoyerAlerteIoT(Incident incident) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reference", incident.getReference());
        metadata.put("type", incident.getType().name());
        metadata.put("gravite", incident.getGravite().name());
        metadata.put("localisation", incident.getLocalisation().getLatitude() + "," + incident.getLocalisation().getLongitude());
        metadata.put("donneesCapteur", incident.getDescription());

        NotificationEvent event = buildBaseEvent(incident, "INCIDENT_IOT_ALERTE", "SMS", "HIGH")
                .metadata(metadata)
                .build();

        notificationProducer.envoyerNotification(event);
    }

    @Override
    public void envoyerEscalade(Incident incident, String motif) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reference", incident.getReference());
        metadata.put("motif", motif);
        metadata.put("type", incident.getType().name());
        metadata.put("gravite", incident.getGravite().name());
        metadata.put("localisation", incident.getLocalisation().getLatitude() + "," + incident.getLocalisation().getLongitude());
        metadata.put("responsableActuel", incident.getResponsableId() != null ? incident.getResponsableId().toString() : "NON_ASSIGNE");

        NotificationEvent event = buildBaseEvent(incident, "INCIDENT_ESCALADE", "EMAIL", "HIGH")
                .metadata(metadata)
                .build();

        notificationProducer.envoyerNotification(event);
    }
}
