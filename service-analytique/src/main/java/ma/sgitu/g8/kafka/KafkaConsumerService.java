package ma.sgitu.g8.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.ingestion.IngestionService;
import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SourceType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final IngestionService ingestionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "g7.position.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeVehiclePosition(String message) {
        log.info("Received vehicle position update from G7: {}", message);
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            payload.putIfAbsent("timestamp", LocalDateTime.now().toString());
            
            ingestionService.ingest(Collections.singletonList(payload), SourceType.VEHICLE);
        } catch (Exception e) {
            log.error("Failed to process vehicle position event", e);
        }
    }

    @KafkaListener(topics = "g7.incident.signale", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeIncidentSignal(String message) {
        log.info("Received incident signal from G7: {}", message);
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            payload.putIfAbsent("timestamp", LocalDateTime.now().toString());
            
            ingestionService.ingest(Collections.singletonList(payload), SourceType.INCIDENT);
        } catch (Exception e) {
            log.error("Failed to process incident signal event", e);
        }
    }

    @KafkaListener(topics = "payment.transaction.completed", groupId = "${spring.kafka.consumer.group-id}")
    public void consumePaymentTransaction(String message) {
        log.info("Received payment transaction from Payment Service: {}", message);
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            payload.putIfAbsent("timestamp", LocalDateTime.now().toString());
            
            ingestionService.ingest(Collections.singletonList(payload), SourceType.PAYMENT);
        } catch (Exception e) {
            log.error("Failed to process payment transaction event", e);
        }
    }
}
