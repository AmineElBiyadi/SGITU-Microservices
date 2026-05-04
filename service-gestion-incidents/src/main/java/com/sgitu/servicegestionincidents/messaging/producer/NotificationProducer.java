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
        log.info("Publication notification pour incident {} vers {}",
                event.getReferenceIncident(), event.getDestinataireId());
        kafkaTemplate.send(MessagingConstants.NOTIFICATION_TOPIC, event);
        log.info("Notification publiée avec succès");
    }
}
