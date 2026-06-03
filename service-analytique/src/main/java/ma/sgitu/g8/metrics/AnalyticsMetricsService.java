package ma.sgitu.g8.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.SnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsMetricsService {

    private static final String UNKNOWN_ZONE = "unknown";

    private final MeterRegistry meterRegistry;
    private final SnapshotRepository snapshotRepository;
    private final EventRepository eventRepository;

    private final Map<String, AtomicReference<Double>> scalarGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<Double>> zoneGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<Double>> ingestionGauges = new ConcurrentHashMap<>();

    public void refreshMetrics() {
        registerScalar("sgitu_freq_total_validations", snapshotValue("FREQ_TOTAL_VALIDATIONS"));
        registerScalar("sgitu_freq_avg_daily", snapshotValue("FREQ_AVG_DAILY"));
        registerScalar("sgitu_veh_punctuality_rate", snapshotValue("VEH_PUNCTUALITY"));
        registerScalar("sgitu_veh_active_count", snapshotValue("VEH_ACTIVE_COUNT"));
        registerScalar("sgitu_inc", snapshotValue("INC_TOTAL"));
        registerScalar("sgitu_inc_repeat_zones", snapshotValue("INC_REPEAT_ZONES"));
        registerScalar("sgitu_pred_peak_hour_score", extractPeakHourScore());
        registerScalar("sgitu_pred_incident_risk_score", extractIncidentRiskScore());

        refreshZoneMetrics(extractZoneCounts("INC_BY_ZONE"));
        refreshIngestionMetrics();
    }

    private void registerScalar(String name, double value) {
        AtomicReference<Double> holder = scalarGauges.computeIfAbsent(name, key -> {
            AtomicReference<Double> ref = new AtomicReference<>(0.0);
            Gauge.builder(name, ref, r -> r.get())
                    .description("SGITU G8 analytics metric")
                    .register(meterRegistry);
            return ref;
        });
        holder.set(value);
    }

    private void refreshZoneMetrics(Map<String, Double> countsByZone) {
        zoneGauges.keySet().stream()
                .filter(zone -> !countsByZone.containsKey(zone))
                .forEach(zone -> zoneGauges.get(zone).set(0.0));

        countsByZone.forEach((zone, count) -> {
            AtomicReference<Double> holder = zoneGauges.computeIfAbsent(zone, key -> {
                AtomicReference<Double> ref = new AtomicReference<>(0.0);
                Gauge.builder("sgitu_inc_by_zone", ref, r -> r.get())
                        .description("Incident count by coordinate zone")
                        .tag("zone", key)
                        .register(meterRegistry);
                return ref;
            });
            holder.set(count);
        });
    }

    private void refreshIngestionMetrics() {
        for (SourceType sourceType : SourceType.values()) {
            String sourceLabel = toSourceLabel(sourceType);
            long count = eventRepository.countBySourceType(sourceType);
            AtomicReference<Double> holder = ingestionGauges.computeIfAbsent(sourceLabel, key -> {
                AtomicReference<Double> ref = new AtomicReference<>(0.0);
                Gauge.builder("sgitu_events_ingested_total", ref, r -> r.get())
                        .description("Total ingested events by upstream service")
                        .tags(Tags.of("source", sourceLabel))
                        .register(meterRegistry);
                return ref;
            });
            holder.set((double) count);
        }
    }

    private double snapshotValue(String statId) {
        return snapshotRepository.findFirstByStatIdOrderByComputedAtDesc(statId)
                .map(this::numericValue)
                .orElse(0.0);
    }

    private double extractPeakHourScore() {
        return snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("PRED_01")
                .map(snapshot -> {
                    Map<String, Object> metadata = snapshot.getMetadata();
                    if (metadata == null) {
                        return 0.0;
                    }
                    Object distribution = metadata.get("distribution");
                    if (!(distribution instanceof List<?> entries)) {
                        return 0.0;
                    }
                    return entries.stream()
                            .filter(Map.class::isInstance)
                            .map(Map.class::cast)
                            .mapToDouble(entry -> readDouble(entry.get("score")))
                            .max()
                            .orElse(0.0);
                })
                .orElse(0.0);
    }

    private double extractIncidentRiskScore() {
        return snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("PRED_02")
                .map(snapshot -> {
                    Map<String, Object> metadata = snapshot.getMetadata();
                    if (metadata == null) {
                        return 0.0;
                    }
                    Object zones = metadata.get("at_risk_zones");
                    if (!(zones instanceof List<?> entries)) {
                        return 0.0;
                    }
                    return entries.stream()
                            .filter(Map.class::isInstance)
                            .map(Map.class::cast)
                            .mapToDouble(entry -> readDouble(entry.get("riskScore")))
                            .max()
                            .orElse(0.0);
                })
                .orElse(0.0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> extractZoneCounts(String statId) {
        Optional<StatSnapshot> snapshot = snapshotRepository.findFirstByStatIdOrderByComputedAtDesc(statId);
        if (snapshot.isEmpty() || snapshot.get().getMetadata() == null) {
            return Map.of();
        }

        Object data = snapshot.get().getMetadata().get("data");
        if (!(data instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }

        Object byZone = rawMap.get("byZone");
        if (byZone instanceof List<?> entries) {
            Map<String, Double> counts = new HashMap<>();
            for (Object entry : entries) {
                if (!(entry instanceof Map<?, ?> zoneEntry)) {
                    continue;
                }
                String zone = zoneEntry.get("zone") == null ? UNKNOWN_ZONE : String.valueOf(zoneEntry.get("zone"));
                counts.put(zone, readDouble(zoneEntry.get("count")));
            }
            return counts;
        }

        // Legacy flat map (keys must not contain dots)
        Map<String, Double> counts = new HashMap<>();
        rawMap.forEach((key, value) -> {
            if ("byZone".equals(key) || "total".equals(key)) {
                return;
            }
            String zone = key == null ? UNKNOWN_ZONE : String.valueOf(key);
            if (value instanceof Number number) {
                counts.put(zone, number.doubleValue());
            } else {
                try {
                    counts.put(zone, Double.parseDouble(String.valueOf(value)));
                } catch (NumberFormatException ex) {
                    log.debug("Skipping non-numeric zone count for {}: {}", zone, value);
                }
            }
        });
        return counts;
    }

    private double numericValue(StatSnapshot snapshot) {
        return snapshot.getValue() == null ? 0.0 : snapshot.getValue();
    }

    private double readDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private String toSourceLabel(SourceType sourceType) {
        return switch (sourceType) {
            case USER -> "G1";
            case TICKETING -> "G2";
            case SUBSCRIPTION -> "G3";
            case PAYMENT -> "G4";
            case VEHICLE -> "G6";
            case INCIDENT -> "G7";
        };
    }
}
