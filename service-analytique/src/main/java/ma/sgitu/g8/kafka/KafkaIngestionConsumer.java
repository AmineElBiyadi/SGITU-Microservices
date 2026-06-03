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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public void consumeTicketingEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        consume(Collections.singletonList(event), SourceType.TICKETING, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.subscription:g3-subscription-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSubscriptionEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        consume(Collections.singletonList(event), SourceType.SUBSCRIPTION, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.payment:g4-payment-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        consume(Collections.singletonList(event), SourceType.PAYMENT, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.vehicle:g6-vehicle-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeVehicleEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        consume(Collections.singletonList(event), SourceType.VEHICLE, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.incident:g7-incident-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeIncidentEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        consume(Collections.singletonList(event), SourceType.INCIDENT, ack);
    }

    @KafkaListener(
            topics = "${kafka.topics.user:g1-user-events}",
            groupId = "${spring.kafka.consumer.group-id:g8-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        consume(Collections.singletonList(event), SourceType.USER, ack);
    }

    private void consume(List<Map<String, Object>> events, SourceType sourceType, Acknowledgment ack) {
        try {
            var normalizedEvents = withSchemaVersion(events);
            var result = ingestionService.ingest(normalizedEvents, sourceType);
            log.info("Kafka [{}] — accepted={} rejected={} status={}",
                    sourceType, result.getTotalAccepted(), result.getTotalRejected(), result.getStatus());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Kafka [{}] — failed to process batch, will not acknowledge", sourceType, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asEventList(Object payload) {
        if (payload instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(event -> (Map<String, Object>) event)
                    .collect(Collectors.toList());
        }
        if (payload instanceof Map<?, ?> map) {
            return Collections.singletonList((Map<String, Object>) map);
        }
        return null;
    }

    private List<Map<String, Object>> withSchemaVersion(List<Map<String, Object>> events) {
        if (events == null) {
            return null;
        }
        return events.stream()
                .map(event -> {
                    if (event == null) {
                        return null;
                    }
                    Map<String, Object> normalized = new LinkedHashMap<>(event);
                    normalized.putIfAbsent("schemaVersion", 1);
                    return normalized;
                })
                .collect(Collectors.toList());
    }
}
