package com.sgitu.servicegestionincidents.messaging.producer;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import com.sgitu.servicegestionincidents.messaging.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public void envoyerNotification(NotificationEvent event) {
        log.info("Publication notification pour incident {} vers {}",
                event.getReferenceIncident(), event.getDestinataireId());
        rabbitTemplate.convertAndSend(
                MessagingConstants.INCIDENT_EXCHANGE,
                MessagingConstants.NOTIFICATION_ROUTING_KEY,
                event
        );
        log.info("Notification publiée avec succès");
    }
}
