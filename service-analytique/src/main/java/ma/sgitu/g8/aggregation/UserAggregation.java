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
import java.util.stream.IntStream;

@Slf4j
@Service
public class UserAggregation {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SnapshotService snapshotService;

    public void compute() {
        computeDailyActiveUsers();
        computePlatformDistribution();
        computeActiveInactiveRatio();
        computeWeeklyNewUserTrend();
    }

    /**
     * USR_01 — Daily Active Users
     * Counts distinct users who had a USER_ACTIVE event today.
     */
    public void computeDailyActiveUsers() {
        try {
            log.info("Computing USR_01 daily_active_users");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> events = events(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            long activeCount = events.stream()
                    .filter(event -> "USER_ACTIVE".equals(event.getEventType()))
                    .map(IncomingEvent::getSourceId)
                    .distinct()
                    .count();

            save("USR_DAU", "USR_01", "DAY", today.toString(), activeCount,
                    Map.of("daily_active_users", activeCount, "date", today.toString()));
        } catch (Exception ex) {
            log.error("Failed to compute USR_01 daily_active_users", ex);
        }
    }

    /**
     * USR_02 — Platform Distribution
     * Breaks down active users by deviceOS (iOS / Android / WEB / UNKNOWN).
     */
    public void computePlatformDistribution() {
        try {
            log.info("Computing USR_02 platform_distribution");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> events = events(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());

            Map<String, Long> counts = events.stream()
                    .filter(event -> "USER_ACTIVE".equals(event.getEventType()))
                    .collect(Collectors.groupingBy(
                            event -> payload(event, "deviceOS", "UNKNOWN").toUpperCase(),
                            LinkedHashMap::new,
                            Collectors.counting()
                    ));

            double total = counts.values().stream().mapToLong(Long::longValue).sum();
            Map<String, Double> distribution = new LinkedHashMap<>();
            counts.forEach((platform, count) ->
                    distribution.put(platform, total == 0 ? 0 : count * 100.0 / total));

            save("USR_PLATFORM_DIST", "USR_02", "WEEK", weekPeriod(today), total,
                    Map.of("platform_distribution", distribution, "total_active_sessions", total));
        } catch (Exception ex) {
            log.error("Failed to compute USR_02 platform_distribution", ex);
        }
    }

    /**
     * USR_03 — Active vs Inactive Ratio
     * Compares USER_ACTIVE vs USER_INACTIVE events this week.
     */
    public void computeActiveInactiveRatio() {
        try {
            log.info("Computing USR_03 active_inactive_ratio");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> events = events(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());

            long active = events.stream()
                    .filter(event -> "USER_ACTIVE".equals(event.getEventType()))
                    .count();
            long inactive = events.stream()
                    .filter(event -> "USER_INACTIVE".equals(event.getEventType()))
                    .count();
            long total = active + inactive;
            double activeRate = total == 0 ? 0 : active * 100.0 / total;

            save("USR_ACTIVE_RATIO", "USR_03", "WEEK", weekPeriod(today), activeRate,
                    Map.of("active", active, "inactive", inactive, "total", total, "active_rate_pct", activeRate));
        } catch (Exception ex) {
            log.error("Failed to compute USR_03 active_inactive_ratio", ex);
        }
    }

    /**
     * USR_04 — Daily Active Users Trend (last 7 days)
     * One data point per day showing distinct active user count.
     */
    public void computeWeeklyNewUserTrend() {
        try {
            log.info("Computing USR_04 weekly_dau_trend");
            LocalDate today = LocalDate.now();
            LocalDate start = today.minusDays(6);
            List<IncomingEvent> events = events(start.atStartOfDay(), today.plusDays(1).atStartOfDay());

            // Group active events by date → distinct user count per day
            Map<String, Long> activeByDate = events.stream()
                    .filter(event -> "USER_ACTIVE".equals(event.getEventType()))
                    .collect(Collectors.groupingBy(
                            event -> event.getTimestamp().toLocalDate().toString(),
                            Collectors.collectingAndThen(
                                    Collectors.mapping(IncomingEvent::getSourceId, Collectors.toSet()),
                                    set -> (long) set.size()
                            )
                    ));

            List<Map<String, Object>> trend = IntStream.range(0, 7)
                    .mapToObj(start::plusDays)
                    .map(date -> {
                        Map<String, Object> point = new LinkedHashMap<>();
                        point.put("date", date.toString());
                        point.put("dau", activeByDate.getOrDefault(date.toString(), 0L));
                        return point;
                    })
                    .toList();

            long totalThisWeek = trend.stream().mapToLong(p -> (Long) p.get("dau")).sum();
            save("USR_DAU_TREND", "USR_04", "WEEK", weekPeriod(today), totalThisWeek,
                    Map.of("dau_trend", trend, "total_active_this_week", totalThisWeek));
        } catch (Exception ex) {
            log.error("Failed to compute USR_04 weekly_dau_trend", ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private List<IncomingEvent> events(LocalDateTime from, LocalDateTime to) {
        return eventRepository.findBySourceTypeAndTimestampBetween(SourceType.USER, from, to);
    }

    private void save(String statId, String displayId, String granularity, String period,
                      double value, Map<String, Object> data) {
        StatSnapshot snapshot = StatSnapshot.builder()
                .snapshotType(SnapshotType.USERS)
                .statId(statId)
                .granularity(granularity)
                .period(period)
                .value(value)
                .metadata(Map.of("id", displayId, "data", data))
                .isPrediction(false)
                .build();
        snapshotService.upsert(statId, SnapshotType.USERS, snapshot);
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
