package com.sgitu.servicegestionincidents.messaging.producer;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import com.sgitu.servicegestionincidents.messaging.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void envoyerNotification(NotificationEvent event) {
        try {
            log.info("Publication notification type {} avec l'ID {}",
                    event.getEventType(), event.getNotificationId());
            kafkaTemplate.send(MessagingConstants.NOTIFICATION_TOPIC, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Échec envoi notification G5 (Kafka indisponible): {}", ex.getMessage());
                        } else {
                            log.info("Notification publiée avec succès");
                        }
                    });
        } catch (Exception e) {
            log.warn("Kafka indisponible — Notification G5 non envoyée: {}", e.getMessage());
        }
    }
}
