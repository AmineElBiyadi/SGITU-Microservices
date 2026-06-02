package ma.sgitu.g8.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.dead-letter:g8-analytics-dlt}")
    private String deadLetterTopic;

    public void publishFailedEvent(List<Map<String, Object>> events, String sourceType, String reason, Throwable exception) {
        Map<String, Object> deadLetter = new HashMap<>();
        deadLetter.put("originalEvents", events);
        deadLetter.put("sourceType", sourceType);
        deadLetter.put("failureReason", reason);
        deadLetter.put("exceptionMessage", exception != null ? exception.getMessage() : null);
        deadLetter.put("exceptionClass", exception != null ? exception.getClass().getName() : null);
        deadLetter.put("timestamp", LocalDateTime.now().toString());
        deadLetter.put("service", "g8-analytics-service");
        deadLetter.put("retryCount", 0);

        kafkaTemplate.send(deadLetterTopic, sourceType, deadLetter)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send to DLT: {}", ex.getMessage());
                    } else {
                        log.info("Event sent to DLT: {} partition={} offset={}",
                                sourceType, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishFailedRecord(ConsumerRecord<?, ?> record, Exception exception) {
        Map<String, Object> deadLetter = new HashMap<>();
        deadLetter.put("originalRecord", record.value());
        deadLetter.put("topic", record.topic());
        deadLetter.put("partition", record.partition());
        deadLetter.put("offset", record.offset());
        deadLetter.put("key", record.key());
        deadLetter.put("exceptionMessage", exception != null ? exception.getMessage() : null);
        deadLetter.put("exceptionClass", exception != null ? exception.getClass().getName() : null);
        deadLetter.put("timestamp", LocalDateTime.now().toString());
        deadLetter.put("service", "g8-analytics-service");

        kafkaTemplate.send(deadLetterTopic, record.key() != null ? record.key().toString() : "unknown", deadLetter)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send to DLT: {}", ex.getMessage());
                    } else {
                        log.info("Record sent to DLT: topic={} partition={} offset={}",
                                record.topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
