package com.serviceabonnement.producer;

import com.serviceabonnement.dto.NotificationEventDTO;
import com.serviceabonnement.dto.RecipientDTO;
import com.serviceabonnement.dto.enums.NotificationChannel;
import com.serviceabonnement.dto.enums.NotificationEventType;
import com.serviceabonnement.dto.enums.NotificationPriority;
import com.serviceabonnement.dto.external.UserDTO;
import com.serviceabonnement.entity.Abonnement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventPublisher {

    private final KafkaTemplate<String, NotificationEventDTO> kafkaTemplate;

    @Value("${application.topics.souscription}")
    private String topic;

    private static final String SOURCE_SERVICE = "SUBSCRIPTION";

    // --- 1. CONFIRMATION_SOUSCRIPTION ---
    public void publishConfirmationSouscription(UserDTO user, Abonnement abonnement) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", abonnement.getPlan().getNomPlan());
        metadata.put("categorie", abonnement.getPlan().getCategorie());
        metadata.put("dateDebut", abonnement.getDateDebut());
        metadata.put("dateFin", abonnement.getDateFin());
        metadata.put("montantPaye", abonnement.getPrixPaye());
        metadata.put("renouvellementAuto", abonnement.isRenouvellementAuto());

        send(NotificationEventType.CONFIRMATION_SOUSCRIPTION, user, metadata, NotificationPriority.HIGH);
    }

    // --- 2. ECHEC_SOUSCRIPTION ---
    public void publishEchecSouscription(UserDTO user, String planNom, String motif) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", planNom);
        metadata.put("motif", motif);

        send(NotificationEventType.ECHEC_SOUSCRIPTION, user, metadata, NotificationPriority.HIGH);
    }

    // --- 3. RAPPEL_EXPIRATION ---
    public void publishRappelExpiration(UserDTO user, Abonnement abonnement, long joursRestants) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", abonnement.getPlan().getNomPlan());
        metadata.put("dateFin", abonnement.getDateFin());
        metadata.put("joursRestants", joursRestants);
        metadata.put("renouvellementAuto", abonnement.isRenouvellementAuto());

        send(NotificationEventType.RAPPEL_EXPIRATION, user, metadata, NotificationPriority.NORMAL);
    }

    // --- 4. RENOUVELLEMENT_EFFECTUE ---
    public void publishRenouvellementEffectue(UserDTO user, Abonnement abonnement, String typeRenouvellement) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", abonnement.getPlan().getNomPlan());
        metadata.put("typeRenouvellement", typeRenouvellement);
        metadata.put("nouvelleDateFin", abonnement.getDateFin());
        metadata.put("montantPaye", abonnement.getPrixPaye());
        metadata.put("dateRenouvellement", java.time.LocalDateTime.now());

        send(NotificationEventType.RENOUVELLEMENT_EFFECTUE, user, metadata, NotificationPriority.HIGH);
    }

    // --- 5. RENOUVELLEMENT_ECHOUE ---
    public void publishRenouvellementEchoue(UserDTO user, String planNom, String motif, int nbTentatives, int maxTentatives, java.time.LocalDateTime dateProchaineRelance) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", planNom);
        metadata.put("motif", motif);
        metadata.put("nbTentatives", nbTentatives);
        metadata.put("maxTentatives", maxTentatives);
        metadata.put("dateProchaineRelance", dateProchaineRelance);

        send(NotificationEventType.RENOUVELLEMENT_ECHOUE, user, metadata, NotificationPriority.HIGH);
    }

    // --- 6. ANNULATION_EFFECTUEE ---
    public void publishAnnulationEffectuee(UserDTO user, Abonnement abonnement, Double montantRembourse, String remboursementId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", abonnement.getPlan().getNomPlan());
        metadata.put("dateAnnulation", java.time.LocalDateTime.now());
        metadata.put("montantRembourse", montantRembourse);
        metadata.put("remboursementId", remboursementId);

        send(NotificationEventType.ANNULATION_EFFECTUEE, user, metadata, NotificationPriority.HIGH);
    }

    // --- 7. ANNULATION_ECHOUE ---
    public void publishAnnulationEchoue(UserDTO user, String planNom, String motif) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", planNom);
        metadata.put("motif", motif);

        send(NotificationEventType.ANNULATION_ECHOUE, user, metadata, NotificationPriority.NORMAL);
    }

    // --- 8. SUSPENSION_EFFECTUEE ---
    public void publishSuspensionEffectuee(UserDTO user, String planNom, String adminId, String motif, java.time.LocalDateTime dateDebutSuspension) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", planNom);
        metadata.put("adminId", adminId);
        metadata.put("motif", motif);
        metadata.put("dateDebutSuspension", dateDebutSuspension);

        send(NotificationEventType.SUSPENSION_EFFECTUEE, user, metadata, NotificationPriority.HIGH);
    }

    // --- 9. SUSPENSION_ECHOUE ---
    public void publishSuspensionEchoue(UserDTO user, String planNom, String adminId, String motif) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", planNom);
        metadata.put("adminId", adminId);
        metadata.put("motif", motif);

        send(NotificationEventType.SUSPENSION_ECHOUE, user, metadata, NotificationPriority.NORMAL);
    }

    // --- 10. DESACTIVATION_EFFECTUEE ---
    public void publishDesactivationEffectuee(UserDTO user, String planNom, java.time.LocalDateTime debut, java.time.LocalDateTime fin, int usees, int max) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", planNom);
        metadata.put("dateDebutDesactivation", debut);
        metadata.put("dateFinDesactivation", fin);
        metadata.put("nbDesactivationsUsees", usees);
        metadata.put("maxDesactivations", max);

        send(NotificationEventType.DESACTIVATION_EFFECTUEE, user, metadata, NotificationPriority.NORMAL);
    }

    // --- 11. DESACTIVATION_ECHOUEE ---
    public void publishDesactivationEchouee(UserDTO user, String planNom, String motif) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", planNom);
        metadata.put("motif", motif);

        send(NotificationEventType.DESACTIVATION_ECHOUEE, user, metadata, NotificationPriority.NORMAL);
    }

    // --- 12. MODIFICATION_EFFECTUEE ---
    public void publishModificationEffectuee(UserDTO user, String planNom, Object changements, Object impact) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", planNom);
        metadata.put("changements", changements);
        metadata.put("impactSurAbonnement", impact);

        send(NotificationEventType.MODIFICATION_EFFECTUEE, user, metadata, NotificationPriority.NORMAL);
    }

    // --- 13. MODIFICATION_ECHOUE ---
    public void publishModificationEchoue(UserDTO user, String planNom, String motif) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("planNom", planNom);
        metadata.put("motif", motif);

        send(NotificationEventType.MODIFICATION_ECHOUE, user, metadata, NotificationPriority.NORMAL);
    }

    // --- CORE SEND METHOD ---
    private void send(NotificationEventType type, UserDTO user, Map<String, Object> metadata, NotificationPriority priority) {
        RecipientDTO recipient = RecipientDTO.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .phone(user.getProfile() != null ? user.getProfile().getPhone() : null)
                .deviceToken(null) // Non présent dans UserDTO actuellement
                .build();

        NotificationEventDTO event = NotificationEventDTO.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService(SOURCE_SERVICE)
                .eventType(type)
                .channel(NotificationChannel.EMAIL) // Par défaut EMAIL, à adapter si besoin
                .priority(priority)
                .recipient(recipient)
                .metadata(metadata)
                .build();

        log.info("Publishing {} event for user {}", type, user.getId());
        kafkaTemplate.send(topic, event);
    }
}
