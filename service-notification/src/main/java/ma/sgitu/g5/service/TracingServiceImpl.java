package ma.sgitu.g5.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TracingServiceImpl - Implémentation du service de traçabilité
 * 
 * Envoie les informations de traçabilité à G10 (Auth Service) via Kafka
 * pour validation asynchrone des tokens JWT émis par les autres groupes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TracingServiceImpl implements ITracingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.template.default-topic:token-validation}")
    private String tracingTopic;

    @Override
    public void sendTracingInfo(String traceId, String token, String sourceGroup, 
                                String tokenSourceService, String userId, 
                                List<String> roles, boolean tokenValid) {
        
        // Construire l'événement de traçabilité
        Map<String, Object> tracingEvent = new HashMap<>();
        tracingEvent.put("traceId", traceId);
        tracingEvent.put("timestamp", LocalDateTime.now().toString());
        tracingEvent.put("targetService", "G5_NOTIFICATION");
        
        // Informations sur le token
        tracingEvent.put("tokenHash", hashToken(token));  // Hash du token pour sécurité
        tracingEvent.put("tokenValid", tokenValid);
        tracingEvent.put("tokenSourceService", tokenSourceService);
        
        // Informations sur la source
        tracingEvent.put("headerSourceGroup", sourceGroup);
        
        // Informations utilisateur
        tracingEvent.put("userId", userId);
        tracingEvent.put("roles", roles);
        
        // Traitement spécial pour G3
        if (sourceGroup != null && sourceGroup.equals("G3")) {
            tracingEvent.put("specialHandling", "G3_USERS");
            tracingEvent.put("priority", "HIGH");
        }

        // Logger localement pour audit immédiat
        log.info("[TRACING] Trace ID: {} - Source: {} - User: {} - Token Valid: {} - Sending to G10", 
                traceId, sourceGroup, userId, tokenValid);

        try {
            // Envoyer à G10 via Kafka pour validation asynchrone
            kafkaTemplate.send(tracingTopic, traceId, tracingEvent);
            log.debug("[TRACING] Trace ID: {} - Événement envoyé à G10 avec succès", traceId);
        } catch (Exception e) {
            log.error("[TRACING] Trace ID: {} - Erreur envoi à G10: {}", traceId, e.getMessage());
            // Ne pas bloquer la requête si l'envoi échoue
        }
    }

    /**
     * Hash le token pour ne pas stocker le token en clair dans les logs
     */
    private String hashToken(String token) {
        if (token == null) return "null";
        return Integer.toHexString(token.hashCode());
    }
}
