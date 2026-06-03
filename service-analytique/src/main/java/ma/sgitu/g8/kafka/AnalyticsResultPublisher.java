package ma.sgitu.g8.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsResultPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.analytics-results:g8-analytics-results}")
    private String analyticsResultsTopic;

    @Value("${kafka.topics.ml-predictions:g8-ml-predictions}")
    private String mlPredictionsTopic;

    public void publishAggregation(String statId, double value, String statType, Map<String, Object> metadata) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "AGGREGATION");
        result.put("statId", statId);
        result.put("value", value);
        result.put("statType", statType);
        result.put("metadata", metadata);
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("service", "g8-analytics-service");

        kafkaTemplate.send(analyticsResultsTopic, statId, result)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish aggregation: {}", ex.getMessage());
                    } else {
                        log.info("Aggregation published: {} partition={} offset={}",
                                statId, res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
                    }
                });
    }

    public void publishMLPrediction(String modelName, double prediction, String predictionType, Map<String, Object> features) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "ML_PREDICTION");
        result.put("modelName", modelName);
        result.put("prediction", prediction);
        result.put("predictionType", predictionType);
        result.put("features", features);
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("service", "g8-analytics-service");

        kafkaTemplate.send(mlPredictionsTopic, modelName, result)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ML prediction: {}", ex.getMessage());
                    } else {
                        log.info("ML prediction published: {} partition={} offset={}",
                                modelName, res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
                    }
                });
    }

    public void publishAlert(String alertType, String severity, String message, Map<String, Object> context) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "ALERT");
        alert.put("alertType", alertType);
        alert.put("severity", severity);
        alert.put("message", message);
        alert.put("context", context);
        alert.put("timestamp", LocalDateTime.now().toString());
        alert.put("service", "g8-analytics-service");

        kafkaTemplate.send(analyticsResultsTopic, alertType, alert)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish alert: {}", ex.getMessage());
                    } else {
                        log.info("Alert published: {} partition={} offset={}",
                                alertType, res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
                    }
                });
    }
}
