package ma.sgitu.g8.ml;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.SnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MlPredictionService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    /**
     * Queries TICKETING events from the last 30 days, filters validated ones,
     * groups by hour, and POSTs to /predict/peak-hours. Stores the result as
     * a PRED_01 StatSnapshot.
     */
    public void computePeakHoursPrediction() {
        try {
            LocalDateTime from = LocalDateTime.now().minusDays(30);
            LocalDateTime to   = LocalDateTime.now();

            var events = eventRepository.findBySourceTypeAndTimestampBetween(
                    SourceType.TICKETING, from, to);

            // Filter validated tickets and group by hour of timestamp
            Map<Integer, Long> countsByHour = events.stream()
                    .filter(e -> {
                        Object status = e.getPayload().get("status");
                        return "validated".equals(status);
                    })
                    .collect(Collectors.groupingBy(
                            e -> e.getTimestamp().getHour(),
                            Collectors.counting()
                    ));

            if (countsByHour.isEmpty()) {
                log.warn("No validated ticket data found in the last 30 days — skipping PRED_01");
                return;
            }

            List<Map<String, Object>> data = countsByHour.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> point = new HashMap<>();
                        point.put("hour", entry.getKey());
                        point.put("validationCount", entry.getValue());
                        return point;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("data", data);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    mlServiceUrl + "/predict/peak-hours",
                    requestBody,
                    Map.class
            );

            StatSnapshot snapshot = new StatSnapshot();
            snapshot.setStatId("PRED_01");
            snapshot.setSnapshotType(SnapshotType.PREDICTION);
            snapshot.setComputedAt(LocalDateTime.now());
            snapshot.setMetadata(response);
            snapshot.setPrediction(true);

            snapshotRepository.save(snapshot);
            log.info("PRED_01 (peak hours prediction) saved successfully");

        } catch (Exception e) {
            log.error("Failed to compute peak hours prediction: {}", e.getMessage(), e);
        }
    }

    /**
     * Queries INCIDENT events from the last 30 days, groups by zone,
     * counts incidents and takes the highest severity per zone, then
     * POSTs to /predict/incidents. Stores the result as a PRED_02 StatSnapshot.
     */
    public void computeIncidentPrediction() {
        try {
            LocalDateTime from = LocalDateTime.now().minusDays(30);
            LocalDateTime to   = LocalDateTime.now();

            var events = eventRepository.findBySourceTypeAndTimestampBetween(
                    SourceType.INCIDENT, from, to);

            if (events.isEmpty()) {
                log.warn("No incident data found in the last 30 days — skipping PRED_02");
                return;
            }

            // Severity ranking: higher number = higher severity
            Map<String, Integer> severityWeight = new HashMap<>();
            severityWeight.put("LOW",      1);
            severityWeight.put("MEDIUM",   2);
            severityWeight.put("HIGH",     3);
            severityWeight.put("CRITICAL", 4);

            // Reverse map: weight -> label (for reconstruction)
            Map<Integer, String> weightToSeverity = new HashMap<>();
            severityWeight.forEach((k, v) -> weightToSeverity.put(v, k));

            // zone -> [count]
            Map<String, long[]> zoneCount = new HashMap<>();
            // zone -> max severity weight
            Map<String, Integer> zoneSeverityMax = new HashMap<>();

            for (var event : events) {
                Map<String, Object> payload = event.getPayload();
                String zone     = String.valueOf(payload.getOrDefault("zone", "UNKNOWN"));
                String severity = String.valueOf(payload.getOrDefault("severity", "LOW"))
                                         .toUpperCase();

                zoneCount.computeIfAbsent(zone, k -> new long[]{0})[0]++;
                int weight = severityWeight.getOrDefault(severity, 1);
                zoneSeverityMax.merge(zone, weight, Math::max);
            }

            List<Map<String, Object>> data = zoneCount.entrySet().stream()
                    .map(entry -> {
                        String zone    = entry.getKey();
                        long   count   = entry.getValue()[0];
                        String maxSev  = weightToSeverity.getOrDefault(
                                zoneSeverityMax.get(zone), "LOW");

                        Map<String, Object> point = new HashMap<>();
                        point.put("zone",          zone);
                        point.put("incidentCount", count);
                        point.put("severity",      maxSev);
                        return point;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("data", data);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    mlServiceUrl + "/predict/incidents",
                    requestBody,
                    Map.class
            );

            StatSnapshot snapshot = new StatSnapshot();
            snapshot.setStatId("PRED_02");
            snapshot.setSnapshotType(SnapshotType.PREDICTION);
            snapshot.setComputedAt(LocalDateTime.now());
            snapshot.setMetadata(response);
            snapshot.setPrediction(true);

            snapshotRepository.save(snapshot);
            log.info("PRED_02 (incident zone prediction) saved successfully");

        } catch (Exception e) {
            log.error("Failed to compute incident prediction: {}", e.getMessage(), e);
        }
    }
}
