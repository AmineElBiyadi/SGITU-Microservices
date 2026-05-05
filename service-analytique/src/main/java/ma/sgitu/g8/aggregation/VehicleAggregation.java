package ma.sgitu.g8.aggregation;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.service.SnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VehicleAggregation {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SnapshotService snapshotService;

    public void compute() {
        computeActiveVehiclesCount();
        computeAvgPunctualityRate();
        computeDelayDistribution();
        computeVehicleUtilizationRate();
        computeAvgSpeedByLine();
    }

    public void computeActiveVehiclesCount() {
        try {
            log.info("Computing VEH_01 active_vehicles_count");
            List<IncomingEvent> events = eventRepository.findBySourceTypeAndProcessedFalse(SourceType.VEHICLE);
            long count = events.stream().filter(event -> "VEHICLE_IN_SERVICE".equals(event.getEventType())).count();
            save("VEH_ACTIVE_COUNT", "VEH_01", "REAL_TIME", "now", count, Map.of("active_vehicles_count", count));
        } catch (Exception ex) {
            log.error("Failed to compute VEH_01 active_vehicles_count", ex);
        }
    }

    public void computeAvgPunctualityRate() {
        try {
            log.info("Computing VEH_02 avg_punctuality_rate");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> events = events(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            Map<String, List<IncomingEvent>> byLine = events.stream().collect(Collectors.groupingBy(this::lineId));
            Map<String, Double> detailByLine = new LinkedHashMap<>();

            byLine.forEach((line, lineEvents) -> detailByLine.put(line, punctualityRate(lineEvents)));
            double globalRate = punctualityRate(events);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("avgPunctualityRate", globalRate);
            data.put("byLine", detailByLine);
            save("VEH_PUNCTUALITY", "VEH_02", "DAY", today.toString(), globalRate, data);
        } catch (Exception ex) {
            log.error("Failed to compute VEH_02 avg_punctuality_rate", ex);
        }
    }

    public void computeDelayDistribution() {
        try {
            log.info("Computing VEH_03 delay_distribution");
            LocalDate today = LocalDate.now();
            Map<String, Long> distribution = events(today.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                    .filter(event -> "VEHICLE_IN_SERVICE".equals(event.getEventType()))
                    .filter(event -> number(event.getPayload().get("delayMinutes")) > 0)
                    .collect(Collectors.groupingBy(this::delayBucket, LinkedHashMap::new, Collectors.counting()));

            Map<String, Object> data = new LinkedHashMap<>(distribution);
            save("VEH_DELAY_DIST", "VEH_03", "DAY", today.toString(), distribution.values().stream().mapToLong(Long::longValue).sum(), data);
        } catch (Exception ex) {
            log.error("Failed to compute VEH_03 delay_distribution", ex);
        }
    }

    public void computeVehicleUtilizationRate() {
        try {
            log.info("Computing VEH_04 vehicle_utilization_rate");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> events = events(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            long total = events.size();
            long active = events.stream().filter(event -> "VEHICLE_IN_SERVICE".equals(event.getEventType())).count();
            double rate = total == 0 ? 0 : active * 100.0 / total;
            save("VEH_UTILIZATION", "VEH_04", "DAY", today.toString(), rate,
                    Map.of("vehicle_utilization_rate", rate, "active", active, "total", total));
        } catch (Exception ex) {
            log.error("Failed to compute VEH_04 vehicle_utilization_rate", ex);
        }
    }

    public void computeAvgSpeedByLine() {
        try {
            log.info("Computing VEH_05 avg_speed_by_line");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> events = events(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());
            Map<String, Double> avgByLine = events.stream()
                    .collect(Collectors.groupingBy(this::lineId,
                            LinkedHashMap::new,
                            Collectors.averagingDouble(event -> number(event.getPayload().get("speed")))));

            Map<String, Object> data = new LinkedHashMap<>(avgByLine);
            save("VEH_AVG_SPEED", "VEH_05", "WEEK", today.minusDays(6) + "/" + today,
                    avgByLine.values().stream().mapToDouble(Double::doubleValue).average().orElse(0), data);
        } catch (Exception ex) {
            log.error("Failed to compute VEH_05 avg_speed_by_line", ex);
        }
    }

    private List<IncomingEvent> events(LocalDateTime from, LocalDateTime to) {
        return eventRepository.findBySourceTypeAndTimestampBetween(SourceType.VEHICLE, from, to);
    }

    private double punctualityRate(List<IncomingEvent> events) {
        if (events.isEmpty()) {
            return 0;
        }
        // On-time = in_service with delayMinutes == 0 or absent
        long onTime = events.stream()
                .filter(event -> "VEHICLE_IN_SERVICE".equals(event.getEventType()))
                .filter(event -> number(event.getPayload().get("delayMinutes")) == 0)
                .count();
        long inService = events.stream()
                .filter(event -> "VEHICLE_IN_SERVICE".equals(event.getEventType()))
                .count();
        return inService == 0 ? 0 : onTime * 100.0 / inService;
    }

    private String delayBucket(IncomingEvent event) {
        double delay = number(event.getPayload().get("delayMinutes"));
        if (delay <= 5) {
            return "0-5";
        }
        if (delay <= 15) {
            return "5-15";
        }
        return "plus_15";
    }

    private void save(String statId, String displayId, String granularity, String period, double value, Map<String, Object> data) {
        StatSnapshot snapshot = StatSnapshot.builder()
                .snapshotType(SnapshotType.VEHICLES)
                .statId(statId)
                .granularity(granularity)
                .period(period)
                .value(value)
                .metadata(Map.of("id", displayId, "data", data))
                .isPrediction(false)
                .build();
        snapshotService.upsert(statId, SnapshotType.VEHICLES, snapshot);
    }

    private String status(IncomingEvent event) {
        return payload(event, "status", "").toUpperCase();
    }

    private String lineId(IncomingEvent event) {
        if (event.getLineId() == null || event.getLineId().isBlank()) {
            return "UNKNOWN";
        }
        return event.getLineId();
    }

    private String payload(IncomingEvent event, String key, String defaultValue) {
        Object value = event.getPayload() == null ? null : event.getPayload().get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? 0 : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
