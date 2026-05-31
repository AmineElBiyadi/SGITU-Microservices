package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.client.UtilisateurClient;
import com.sgitu.servicegestionincidents.dto.response.UtilisateurDTO;
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
    private final UtilisateurClient utilisateurClient;

    private NotificationEvent.Recipient buildRecipient(Long userId, String channel) {
        if (userId == null || userId == 0) {
            return NotificationEvent.Recipient.builder()
                    .userId("SYSTEM")
                    .build();
        }

        String email = null;
        String phone = null;

        try {
            UtilisateurDTO user = utilisateurClient.obtenirUtilisateur(userId);
            if (user != null) {
                email = user.getEmail();
                if (user.getProfile() != null) {
                    phone = user.getProfile().getPhone();
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de récupérer les infos de contact pour userId {} depuis G3 : {}", userId, e.getMessage());
            // Fallbacks de résilience en cas de panne de G3
            if ("EMAIL".equals(channel)) email = "fallback@sgitu.ma";
            if ("SMS".equals(channel)) phone = "+212000000000";
        }

        return NotificationEvent.Recipient.builder()
                .userId(userId.toString())
                .email(email)
                .phone(phone)
                .build();
    }

    private NotificationEvent.NotificationEventBuilder buildBaseEvent(String eventType, String channel, String priority, NotificationEvent.Recipient recipient) {
        return NotificationEvent.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService("G9_INCIDENTS")
                .eventType(eventType)
                .channel(channel)
                .priority(priority)
                .recipient(recipient);
    }

    @Override
    public void envoyerConfirmation(Incident incident) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("incidentId", incident.getId());
        metadata.put("reference", incident.getReference());
        metadata.put("type", incident.getType().name());
        metadata.put("dateSignalement", incident.getDateSignalement().toString());

        NotificationEvent.Recipient recipient = buildRecipient(incident.getDeclarantId(), "EMAIL");

        NotificationEvent event = buildBaseEvent("INCIDENT_CONFIRMATION", "EMAIL", "NORMAL", recipient)
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

        NotificationEvent.Recipient recipient = buildRecipient(incident.getDeclarantId(), "PUSH");

        NotificationEvent event = buildBaseEvent("INCIDENT_STATUS_UPDATED", "PUSH", "NORMAL", recipient)
                .metadata(metadata)
                .build();

        notificationProducer.envoyerNotification(event);
    }

    @Override
    public void envoyerAlerteDispatchers(Incident incident) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("incidentId", incident.getId());
        metadata.put("reference", incident.getReference());
        metadata.put("type", incident.getType().name());
        metadata.put("source", incident.getSource());
        metadata.put("localisation", incident.getLocalisation().getLatitude() + "," + incident.getLocalisation().getLongitude());
        metadata.put("dateSignalement", incident.getDateSignalement().toString());
        if (incident.getDescription() != null) {
            metadata.put("description", incident.getDescription());
        }

        try {
            java.util.List<UtilisateurDTO> dispatchers = utilisateurClient.obtenirUtilisateursParRole("ROLE_DISPATCHER");
            for (UtilisateurDTO dispatcher : dispatchers) {
                NotificationEvent.Recipient recipient = buildRecipient(dispatcher.getId(), "PUSH");
                NotificationEvent event = buildBaseEvent("INCIDENT_ALERT", "PUSH", "HIGH", recipient)
                        .metadata(metadata)
                        .build();
                notificationProducer.envoyerNotification(event);
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la récupération des dispatchers pour alerte : {}", e.getMessage());
        }
    }

    @Override
    public void envoyerEscalade(Incident incident, String motif) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reference", incident.getReference());
        metadata.put("motifEscalade", motif);
        metadata.put("responsableActuel", incident.getResponsableId() != null ? incident.getResponsableId().toString() : "NON_ASSIGNE");
        metadata.put("type", incident.getType().name());
        metadata.put("gravite", incident.getGravite().name());
        metadata.put("localisation", incident.getLocalisation().getLatitude() + "," + incident.getLocalisation().getLongitude());

        // 1. Notifier tous les superviseurs
        try {
            java.util.List<UtilisateurDTO> supervisors = utilisateurClient.obtenirUtilisateursParRole("ROLE_SUPERVISOR");
            for (UtilisateurDTO supervisor : supervisors) {
                NotificationEvent.Recipient recipient = buildRecipient(supervisor.getId(), "PUSH");
                NotificationEvent event = buildBaseEvent("INCIDENT_ESCALADE", "PUSH", "HIGH", recipient)
                        .metadata(metadata)
                        .build();
                notificationProducer.envoyerNotification(event);
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la récupération des superviseurs pour escalade : {}", e.getMessage());
        }

        // 2. Notifier le technicien assigné (s'il y en a un)
        if (incident.getResponsableId() != null) {
            NotificationEvent.Recipient technicienRecipient = buildRecipient(incident.getResponsableId(), "PUSH");
            NotificationEvent technicienEvent = buildBaseEvent("INCIDENT_ESCALADE", "PUSH", "HIGH", technicienRecipient)
                    .metadata(metadata)
                    .build();
            notificationProducer.envoyerNotification(technicienEvent);
        }
    }

    @Override
    public void envoyerAssignation(Incident incident) {
        if (incident.getResponsableId() == null) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reference", incident.getReference());
        metadata.put("typePanne", incident.getType().name());
        metadata.put("urgence", incident.getGravite().name());
        metadata.put("localisation", incident.getLocalisation().getLatitude() + "," + incident.getLocalisation().getLongitude());
        if (incident.getDescription() != null) {
            metadata.put("description", incident.getDescription());
        }

        // 1. Envoi PUSH
        NotificationEvent.Recipient recipientPush = buildRecipient(incident.getResponsableId(), "PUSH");
        NotificationEvent eventPush = buildBaseEvent("INTERVENTION_ASSIGNED", "PUSH", "HIGH", recipientPush)
                .metadata(metadata)
                .build();
        notificationProducer.envoyerNotification(eventPush);

        // 2. Envoi SMS
        NotificationEvent.Recipient recipientSms = buildRecipient(incident.getResponsableId(), "SMS");
        NotificationEvent eventSms = buildBaseEvent("INTERVENTION_ASSIGNED", "SMS", "HIGH", recipientSms)
                .metadata(metadata)
                .build();
        notificationProducer.envoyerNotification(eventSms);
    }

    @Override
    public void envoyerAssignationRenfort(Incident incident, Long agentId) {
        if (agentId == null) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reference", incident.getReference());
        metadata.put("typePanne", incident.getType().name());
        metadata.put("urgence", incident.getGravite().name());
        metadata.put("localisation", incident.getLocalisation().getLatitude() + "," + incident.getLocalisation().getLongitude());
        
        String desc = incident.getDescription() != null ? incident.getDescription() : "";
        desc = "Vous avez été appelé en renfort sur cet incident. " + desc;
        metadata.put("description", desc);

        // 1. Envoi PUSH
        NotificationEvent.Recipient recipientPush = buildRecipient(agentId, "PUSH");
        NotificationEvent eventPush = buildBaseEvent("RENFORT_ASSIGNED", "PUSH", "HIGH", recipientPush)
                .metadata(metadata)
                .build();
        notificationProducer.envoyerNotification(eventPush);

        // 2. Envoi SMS
        NotificationEvent.Recipient recipientSms = buildRecipient(agentId, "SMS");
        NotificationEvent eventSms = buildBaseEvent("RENFORT_ASSIGNED", "SMS", "HIGH", recipientSms)
                .metadata(metadata)
                .build();
        notificationProducer.envoyerNotification(eventSms);
    }
}
