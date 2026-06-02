package ma.sgitu.g5.controller;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.request.MetadataDTO;
import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.request.RecipientDTO;
import ma.sgitu.g5.service.INotificationService;

/**
 * KafkaConsumerController — Consommateur centralisé pour SGITU.
 * Utilise des patterns pour capturer dynamiquement tous les topics de G1, G2 et G3.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerController {

    private final INotificationService notificationService;
    private final ObjectMapper objectMapper;

    // ── G1 — Billetterie : Consomme TOUS les topics ticket.* (11 topics) ──
    @KafkaListener(
        topicPattern = "ticket\\..*", 
        groupId = "notification-group", 
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTicketEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("[KAFKA-G1] Event reçu sur topic={} | partition={} | offset={}", 
                record.topic(), record.partition(), record.offset());
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get("eventType");
            String userId = String.valueOf(event.get("userId"));
            String ticketId = String.valueOf(event.getOrDefault("ticketId", "unknown"));

            // Flux G1 exige EMAIL + PUSH pour les notifications de ticket
            List<String> channels = List.of("EMAIL", "PUSH");
            
            Map<String, Object> metadataMap = new HashMap<>(event);
            metadataMap.remove("eventType");
            metadataMap.remove("userId");

            for (String channel : channels) {
                // ID déterministe pour garantir l'idempotence (évite les doublons)
                String deterministicId = "G1-" + ticketId + "-" + eventType + "-" + channel;
                
                NotificationRequestDTO dto = buildDto(
                    deterministicId, "G1_BILLETTERIE", eventType, channel, "NORMAL", userId, event, metadataMap
                );
                notificationService.send(dto);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA-G1] Échec du traitement : {}", e.getMessage());
        }
    }

    // ── G2 — Abonnements : Consomme TOUS les topics abonnement.* (7 topics) ──
    @KafkaListener(
        topicPattern = "abonnement\\..*", 
        groupId = "notification-group", 
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAbonnementEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("[KAFKA-G2] Event reçu sur topic={} | offset={}", record.topic(), record.offset());
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            
            // G2 utilise le champ "type" au lieu de "eventType"
            String eventType = (String) event.getOrDefault("type", "UNKNOWN_ABONNEMENT");
            String userId = String.valueOf(event.get("userId"));
            String abonnementId = String.valueOf(event.getOrDefault("abonnementId", "0"));

            // G2 fournit une liste de canaux (ex: ["EMAIL", "SMS"])
            List<String> channels = (List<String>) event.getOrDefault("channels", List.of("EMAIL"));
            Map<String, Object> data = (Map<String, Object>) event.getOrDefault("data", new HashMap<>());

            for (String channel : channels) {
                String deterministicId = "G2-" + abonnementId + "-" + eventType + "-" + channel;
                
                NotificationRequestDTO dto = buildDto(
                    deterministicId, "G2_ABONNEMENT", eventType, channel, "NORMAL", userId, event, data
                );
                notificationService.send(dto);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA-G2] Échec du traitement : {}", e.getMessage());
        }
    }

    // ── G3 — Utilisateurs : Topic unique user-events ───────────────────────
    @KafkaListener(
        topics = "user-events", 
        groupId = "notification-group", 
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("[KAFKA-G3] Event reçu sur user-events | offset={}", record.offset());
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get("eventType");
            String userId = (String) event.get("userId");

            // G3 utilise principalement l'EMAIL
            String deterministicId = "G3-" + userId + "-" + eventType;

            NotificationRequestDTO dto = buildDto(
                deterministicId, "G3_UTILISATEUR", eventType, "EMAIL", "HIGH", userId, event, event
            );
            notificationService.send(dto);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA-G3] Échec du traitement : {}", e.getMessage());
        }
    }


    // ── G6 — Paiement : notifications paiement via topic payment.notification ──
    @KafkaListener(
        topics = "payment.notification",
        groupId = "notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentNotification(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("[KAFKA-G6] Event paiement recu | topic={} | offset={}", record.topic(), record.offset());
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);

            String notificationId = String.valueOf(event.getOrDefault(
                    "notificationId", "G6-PAYMENT-" + UUID.randomUUID()));
            String eventType = String.valueOf(event.getOrDefault("eventType", "UNKNOWN_PAYMENT"));
            String channel = String.valueOf(event.getOrDefault("channel", "EMAIL"));
            String priority = String.valueOf(event.getOrDefault("priority", "NORMAL"));
            String userId = String.valueOf(resolveRecipientField(event, "userId", "unknown"));

            Map<String, Object> metadata = extractMetadata(event);

            NotificationRequestDTO dto = buildDto(
                notificationId,
                "G6_PAYMENT",
                eventType,
                channel,
                priority,
                userId,
                event,
                metadata
            );

            notificationService.send(dto);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA-G6] Echec du traitement paiement : {}", e.getMessage(), e);
        }
    }

    // ── G9 — Incidents : Topic unique 'notifications' (Contrat v5.0) ───────
    @KafkaListener(
        topics = "${g5.kafka.g9-notifications-topic:notifications}", 
        groupId = "notification-group", 
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleG9Notification(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("[KAFKA-G9] Event reçu sur 'notifications' | offset={}", record.offset());
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get("eventType");
            
            // Canaux : priorité au payload, sinon EMAIL par défaut
            List<String> channels = (List<String>) event.getOrDefault("channels", List.of("EMAIL"));
            String userId = String.valueOf(resolveRecipientField(event, "userId", "unknown"));
            
            Map<String, Object> metadata = extractMetadata(event);
            String reference = String.valueOf(metadata.getOrDefault("reference", "REF-" + UUID.randomUUID().toString().substring(0, 8)));

            for (String channel : channels) {
                // ID déterminisite pour l'idempotence G9
                String deterministicId = "G9-" + reference + "-" + eventType + "-" + channel;
                
                NotificationRequestDTO dto = buildDto(
                    deterministicId, "G9_INCIDENT", eventType, channel, "NORMAL", userId, event, metadata
                );
                notificationService.send(dto);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA-G9] Échec du traitement : {}", e.getMessage());
        }
    }

    // ── Dead Letter Topic : relance des messages en échec ─────────────────
    /**
     * Écoute tous les Dead Letter Topics générés par le {@link ma.sgitu.g5.config.KafkaConfig}.
     * <p>
     * Chaque topic applicatif possède son DLT correspondant (ex: ticket.created → ticket.created.DLT).
     * Ce listener capture ces messages, extrait les métadonnées d'échec depuis les headers Kafka
     * et tente une nouvelle livraison via {@link INotificationService#send}.
     * </p>
     */
    @KafkaListener(
        topicPattern = ".*\\.DLT",
        groupId = "notification-group-dlt",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetterEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String originalTopic = extractHeader(record, KafkaHeaders.DLT_ORIGINAL_TOPIC);
        String exceptionMessage = extractHeader(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        String originalOffset = extractHeader(record, KafkaHeaders.DLT_ORIGINAL_OFFSET);

        log.warn("[KAFKA-DLT] Message reçu sur DLT topic={} | originalTopic={} | offset={} | erreur={}",
                record.topic(), originalTopic, originalOffset, exceptionMessage);

        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);

            // Reconstitution du DTO à partir du payload original
            String eventType = (String) event.getOrDefault("eventType",
                               event.getOrDefault("type", "UNKNOWN_DLT").toString());
            String userId    = String.valueOf(event.getOrDefault("userId", "unknown"));

            // ID déterministe pour éviter les doublons lors de la relance
            String deterministicId = "DLT-" + originalTopic + "-" + originalOffset + "-" + eventType;

            NotificationRequestDTO dto = buildDto(
                deterministicId, "DLT_RETRY", eventType, "EMAIL", "HIGH", userId, event, event
            );
            notificationService.send(dto);

            acknowledgment.acknowledge();
            log.info("[KAFKA-DLT] Message relancé avec succès depuis DLT | id={}", deterministicId);

        } catch (Exception e) {
            log.error("[KAFKA-DLT] Échec de la relance depuis DLT topic={} : {}", record.topic(), e.getMessage(), e);
            // On accuse réception malgré tout pour éviter une boucle infinie sur le DLT
            acknowledgment.acknowledge();
        }
    }

    /**
     * Extrait la valeur d'un header Kafka sous forme de String UTF-8.
     * Retourne "N/A" si le header est absent.
     */
    private String extractHeader(ConsumerRecord<String, String> record, String headerKey) {
        Header header = record.headers().lastHeader(headerKey);
        return (header != null) ? new String(header.value(), StandardCharsets.UTF_8) : "N/A";
    }

    private NotificationRequestDTO buildDto(String id, String source, String type, String channel,
                                          String priority, String userId, Map<String, Object> rawEvent,
                                          Map<String, Object> metadata) {
        NotificationRequestDTO dto = new NotificationRequestDTO();
        dto.setNotificationId(id);
        dto.setSourceService(source);
        dto.setEventType(type);
        dto.setChannel(channel);
        dto.setPriority(priority);
        
        RecipientDTO recipient = new RecipientDTO();
        recipient.setUserId(userId);
        // TODO: Appeler le service G3 (Utilisateurs) pour résoudre l'email/téléphone à partir de l'userId
        recipient.setEmail(String.valueOf(resolveRecipientField(rawEvent, "email", "")));
        recipient.setPhone(String.valueOf(resolveRecipientField(rawEvent, "phone", "")));
        recipient.setDeviceToken(String.valueOf(resolveRecipientField(rawEvent, "deviceToken", "")));
        dto.setRecipient(recipient);

        MetadataDTO m = new MetadataDTO();
        m.setData(metadata);
        dto.setMetadata(m);
        return dto;
    }

    private Object resolveRecipientField(Map<String, Object> rawEvent, String field, String fallback) {
        Object recipientObj = rawEvent.get("recipient");
        if (recipientObj instanceof Map<?, ?> recipientMap) {
            Object value = recipientMap.get(field);
            if (value != null) {
                return value;
            }
        }
        Object direct = rawEvent.get(field);
        return (direct != null) ? direct : fallback;
    }

    private Map<String, Object> extractMetadata(Map<String, Object> rawEvent) {
        Object meta = rawEvent.get("metadata");
        if (meta instanceof Map<?, ?> metaMap) {
            Map<String, Object> casted = new HashMap<>();
            for (Map.Entry<?, ?> entry : metaMap.entrySet()) {
                casted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return casted;
        }
        Map<String, Object> fallback = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawEvent.entrySet()) {
            if (!"recipient".equals(entry.getKey())) {
                fallback.put(entry.getKey(), entry.getValue());
            }
        }
        return fallback;
    }
}
