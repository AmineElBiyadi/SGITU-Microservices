package ma.sgitu.g8.aggregation;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.SnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IncidentAggregation {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

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
            List<IncomingEvent> incidents = events(today.atStartOfDay(), today.plusDays(1).atStartOfDay());

            save("INC_TOTAL", "INC_01", "DAY", today.toString(), incidents.size(),
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
                    .collect(Collectors.groupingBy(this::zoneId, Collectors.counting()));

            save("INC_BY_ZONE", "INC_03", "WEEK", weekPeriod(today), grouped.size(), new LinkedHashMap<>(grouped));
        } catch (Exception ex) {
            log.error("Failed to compute INC_03 incident_by_zone", ex);
        }
    }

    public void computeAvgResolutionTime() {
        try {
            log.info("Computing INC_04 avg_resolution_time");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> closedIncidents = events(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                    .filter(event -> "CLOSED".equals(normalizedPayload(event, "status", "")))
                    .toList();

            double averageMinutes = closedIncidents.stream()
                    .map(this::resolutionMinutes)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
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
                    .collect(Collectors.groupingBy(this::zoneId, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() >= 3)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));

            save("INC_REPEAT_ZONES", "INC_05", "MONTH", month.toString(), repeatedZones.size(), new LinkedHashMap<>(repeatedZones));
        } catch (Exception ex) {
            log.error("Failed to compute INC_05 repeat_incident_zones", ex);
        }
    }

    private List<IncomingEvent> events(LocalDateTime from, LocalDateTime to) {
        return eventRepository.findBySourceTypeAndTimestampBetween(SourceType.INCIDENT, from, to);
    }

    private void save(String statId, String displayId, String granularity, String period, double value, Map<String, Object> data) {
        snapshotRepository.save(StatSnapshot.builder()
                .snapshotType(SnapshotType.INCIDENTS)
                .statId(statId)
                .granularity(granularity)
                .period(period)
                .value(value)
                .metadata(Map.of("id", displayId, "data", data))
                .computedAt(LocalDateTime.now())
                .isPrediction(false)
                .build());
    }

    private Double resolutionMinutes(IncomingEvent event) {
        LocalDateTime resolvedAt = parseDateTime(event.getPayload().get("resolvedAt"));
        LocalDateTime startedAt = event.getTimestamp();
        if (resolvedAt == null || startedAt == null || resolvedAt.isBefore(startedAt)) {
            return null;
        }
        return (double) Duration.between(startedAt, resolvedAt).toMinutes();
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String normalizedPayload(IncomingEvent event, String key, String defaultValue) {
        return payload(event, key, defaultValue).toUpperCase();
    }

    private String incidentType(IncomingEvent event) {
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            return "OTHER";
        }
        return event.getEventType().toUpperCase();
    }

    private String zoneId(IncomingEvent event) {
        if (event.getZoneId() == null || event.getZoneId().isBlank()) {
            return "UNKNOWN";
        }
        return event.getZoneId();
    }

    private String payload(IncomingEvent event, String key, String defaultValue) {
        Object value = event.getPayload() == null ? null : event.getPayload().get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private String weekPeriod(LocalDate today) {
        return today.minusDays(6) + "/" + today;
    }
}
