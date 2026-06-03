package ma.sgitu.g8.aggregation;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.geo.CoordinateExtractor;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IncidentAggregation {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SnapshotService snapshotService;

    public void compute() {
        computeTotalIncidents();
        computeIncidentByType();
        computeIncidentByZone();
        computeAvgResolutionTime();
        computeRepeatIncidentZones();
    }

    public void computeTotalIncidents() {
        try {
            log.info("Computing INC_01 total_incidents");
            LocalDate today = LocalDate.now();
            // Use 7-day window to match data spread from the seed script
            List<IncomingEvent> incidents = events(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());

            save("INC_TOTAL", "INC_01", "WEEK", weekPeriod(today), incidents.size(),
                    Map.of("total_incidents", incidents.size(), "date", today.toString()));
        } catch (Exception ex) {
            log.error("Failed to compute INC_01 total_incidents", ex);
        }
    }

    public void computeIncidentByType() {
        try {
            log.info("Computing INC_02 incident_by_type");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> incidents = events(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());
            Map<String, Long> grouped = incidents.stream()
                    .collect(Collectors.groupingBy(this::incidentType, Collectors.counting()));

            save("INC_BY_TYPE", "INC_02", "WEEK", weekPeriod(today), grouped.size(), new LinkedHashMap<>(grouped));
        } catch (Exception ex) {
            log.error("Failed to compute INC_02 incident_by_type", ex);
        }
    }

    public void computeIncidentByZone() {
        try {
            log.info("Computing INC_03 incident_by_zone");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> incidents = events(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());
            Map<String, Long> grouped = incidents.stream()
                    .collect(Collectors.groupingBy(this::resolveZone, Collectors.counting()));

            save("INC_BY_ZONE", "INC_03", "WEEK", weekPeriod(today), sumCounts(grouped), zoneMetadata(grouped));
        } catch (Exception ex) {
            log.error("Failed to compute INC_03 incident_by_zone", ex);
        }
    }

    public void computeAvgResolutionTime() {
        try {
            log.info("Computing INC_04 avg_resolution_time");
            LocalDate today = LocalDate.now();
            // Accept incidents that have a resolutionMinutes field (regardless of status)
            List<IncomingEvent> resolvedIncidents = events(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                    .filter(event -> event.getPayload() != null && event.getPayload().get("resolutionMinutes") != null)
                    .toList();

            double averageMinutes = resolvedIncidents.stream()
                    .mapToDouble(event -> resolutionMinutes(event))
                    .average()
                    .orElse(0);

            save("INC_AVG_RESOLUTION", "INC_04", "WEEK", weekPeriod(today), averageMinutes,
                    Map.of("avg_resolution_time", averageMinutes, "unit", "minutes"));
        } catch (Exception ex) {
            log.error("Failed to compute INC_04 avg_resolution_time", ex);
        }
    }

    public void computeRepeatIncidentZones() {
        try {
            log.info("Computing INC_05 repeat_incident_zones");
            LocalDate today = LocalDate.now();
            YearMonth month = YearMonth.from(today);
            List<IncomingEvent> incidents = events(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
            Map<String, Long> repeatedZones = incidents.stream()
                    .collect(Collectors.groupingBy(this::resolveZone, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() >= 2)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));

            save("INC_REPEAT_ZONES", "INC_05", "MONTH", month.toString(), repeatedZones.size(), zoneMetadata(repeatedZones));
        } catch (Exception ex) {
            log.error("Failed to compute INC_05 repeat_incident_zones", ex);
        }
    }

    private List<IncomingEvent> events(LocalDateTime from, LocalDateTime to) {
        return eventRepository.findBySourceTypeAndTimestampBetween(SourceType.INCIDENT, from, to)
                .stream()
                .filter(event -> event != null && event.getTimestamp() != null)
                .toList();
    }

    private void save(String statId, String displayId, String granularity, String period, double value, Map<String, Object> data) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", displayId);
        metadata.put("data", data);

        StatSnapshot snapshot = StatSnapshot.builder()
                .schemaVersion(StatSnapshot.CURRENT_SCHEMA_VERSION)
                .snapshotType(SnapshotType.INCIDENTS)
                .statId(statId)
                .granularity(granularity)
                .period(period)
                .value(value)
                .metadata(metadata)
                .isPrediction(false)
                .build();
        snapshotService.upsert(statId, SnapshotType.INCIDENTS, snapshot);
    }

    /**
     * Zone labels contain dots (e.g. "33.57,-7.59"). MongoDB map keys cannot contain "."
     * without breaking nested document paths, so zone breakdowns are stored as a list.
     */
    private Map<String, Object> zoneMetadata(Map<String, Long> countsByZone) {
        List<Map<String, Object>> byZone = countsByZone.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "zone", entry.getKey(),
                        "count", entry.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("byZone", byZone);
        data.put("total", sumCounts(countsByZone));
        return data;
    }

    private long sumCounts(Map<String, Long> countsByZone) {
        return countsByZone.values().stream().mapToLong(Long::longValue).sum();
    }

    private double resolutionMinutes(IncomingEvent event) {
        Object value = event == null || event.getPayload() == null ? null : event.getPayload().get("resolutionMinutes");
        if (value instanceof Number n) {
            return Math.max(0, n.doubleValue());
        }
        try {
            return value == null ? 0 : Math.max(0, Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String normalizedPayload(IncomingEvent event, String key, String defaultValue) {
        return payload(event, key, defaultValue).toUpperCase();
    }

    private String incidentType(IncomingEvent event) {
        if (event == null) {
            return "OTHER";
        }
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            return "OTHER";
        }
        return event.getEventType().toUpperCase();
    }

    private String resolveZone(IncomingEvent event) {
        return CoordinateExtractor.resolveZoneLabel(event);
    }

    private String payload(IncomingEvent event, String key, String defaultValue) {
        Object value = event == null || event.getPayload() == null ? null : event.getPayload().get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private String weekPeriod(LocalDate today) {
        return today.minusDays(6) + "/" + today;
    }
}
