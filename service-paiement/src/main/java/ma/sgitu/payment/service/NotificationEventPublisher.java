package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.dto.request.NotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.payment-notifications:payment.notification}")
    private String notificationTopic;

    public void publishNotification(NotificationRequest request) {
        log.info("Envoi d'un événement de notification Kafka vers le topic {}: eventType={}", notificationTopic, request.getEventType());
        try {
            // Note: we can use eventType as the key for partitioning if needed, but it's optional.
            kafkaTemplate.send(notificationTopic, request.getNotificationId(), request);
            log.info("Événement de notification Kafka envoyé avec succès (ID: {})", request.getNotificationId());
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'événement Kafka: {}", e.getMessage(), e);
        }
    }
}
