package ma.sgitu.g8.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.ingestion.IngestionService;
import ma.sgitu.g8.model.SourceType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaIngestionConsumer {

    private final IngestionService ingestionService;

    @KafkaListener(
            topics = "${kafka.topics.ticketing:g2-ticketing-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTicketingEvents(@Payload List<Map<String, Object>> events, Acknowledgment ack) {
        consume(events, SourceType.TICKETING, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.subscription:g3-subscription-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSubscriptionEvents(@Payload List<Map<String, Object>> events, Acknowledgment ack) {
        consume(events, SourceType.SUBSCRIPTION, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.payment:g4-payment-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvents(@Payload List<Map<String, Object>> events, Acknowledgment ack) {
        consume(events, SourceType.PAYMENT, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.vehicle:g6-vehicle-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeVehicleEvents(@Payload List<Map<String, Object>> events, Acknowledgment ack) {
        consume(events, SourceType.VEHICLE, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.incident:g7-incident-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeIncidentEvents(@Payload List<Map<String, Object>> events, Acknowledgment ack) {
        consume(events, SourceType.INCIDENT, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.user:g1-user-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        // G3 doesn't send schemaVersion; add it for validation compatibility
        if (!event.containsKey("schemaVersion")) {
            event.put("schemaVersion", 1);
        }
        consume(Collections.singletonList(event), SourceType.USER, ack);
    }

    private void consume(List<Map<String, Object>> events, SourceType sourceType, Acknowledgment ack) {
        try {
            var result = ingestionService.ingest(events, sourceType);
            log.info("Kafka [{}] — accepted={} rejected={} status={}",
                    sourceType, result.getTotalAccepted(), result.getTotalRejected(), result.getStatus());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Kafka [{}] — failed to process batch, will not acknowledge", sourceType, ex);
        }
    }
}
