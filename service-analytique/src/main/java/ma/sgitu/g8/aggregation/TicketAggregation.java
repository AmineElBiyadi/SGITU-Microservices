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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class TicketAggregation {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

    public void compute() {
        computeTotalValidations();
        computePeakHourDistribution();
        computePeakHours();
        computeAvgDailyPassengers();
        computeLineUsageRanking();
        computeStationFootfall();
        computeWeekendVsWeekdayRatio();
    }

    public void computeTotalValidations() {
        try {
            log.info("Computing FREQ_01 total_validations");
            LocalDate today = LocalDate.now();
            long count = validatedTickets(today.atStartOfDay(), today.plusDays(1).atStartOfDay()).size();
            save("FREQ_TOTAL_VALIDATIONS", "FREQ_01", "DAY", today.toString(), count,
                    Map.of("total_validations", count, "date", today.toString()));
        } catch (Exception ex) {
            log.error("Failed to compute FREQ_01 total_validations", ex);
        }
    }

    public void computePeakHourDistribution() {
        try {
            log.info("Computing FREQ_02 peak_hour_distribution");
            LocalDate today = LocalDate.now();
            Map<String, Long> distribution = hourlyDistribution(validatedTickets(today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
            save("FREQ_PEAK_HOUR_DIST", "FREQ_02", "DAY", today.toString(),
                    distribution.values().stream().mapToLong(Long::longValue).sum(), new LinkedHashMap<>(distribution));
        } catch (Exception ex) {
            log.error("Failed to compute FREQ_02 peak_hour_distribution", ex);
        }
    }

    public void computePeakHours() {
        try {
            log.info("Computing FREQ_03 peak_hours");
            LocalDate today = LocalDate.now();
            Map<String, Long> distribution = hourlyDistribution(validatedTickets(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()));
            Map<String, Long> topHours = distribution.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                    .limit(3)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));

            save("FREQ_PEAK_HOURS", "FREQ_03", "WEEK", weekPeriod(today),
                    topHours.values().stream().mapToLong(Long::longValue).sum(), new LinkedHashMap<>(topHours));
        } catch (Exception ex) {
            log.error("Failed to compute FREQ_03 peak_hours", ex);
        }
    }

    public void computeAvgDailyPassengers() {
        try {
            log.info("Computing FREQ_04 avg_daily_passengers");
            LocalDate today = LocalDate.now();
            YearMonth month = YearMonth.from(today);
            long total = validatedTickets(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()).size();
            double average = total / (double) today.getDayOfMonth();

            save("FREQ_AVG_DAILY", "FREQ_04", "MONTH", month.toString(), average,
                    Map.of("avg_daily_passengers", average, "total_validations", total));
        } catch (Exception ex) {
            log.error("Failed to compute FREQ_04 avg_daily_passengers", ex);
        }
    }

    public void computeLineUsageRanking() {
        try {
            log.info("Computing FREQ_05 line_usage_ranking");
            LocalDate today = LocalDate.now();
            Map<String, Long> ranking = validatedTickets(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay())
                    .stream()
                    .collect(Collectors.groupingBy(this::lineId, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));

            save("FREQ_LINE_RANKING", "FREQ_05", "WEEK", weekPeriod(today),
                    ranking.values().stream().mapToLong(Long::longValue).sum(), new LinkedHashMap<>(ranking));
        } catch (Exception ex) {
            log.error("Failed to compute FREQ_05 line_usage_ranking", ex);
        }
    }

    public void computeStationFootfall() {
        try {
            log.info("Computing FREQ_06 station_footfall");
            LocalDate today = LocalDate.now();
            Map<String, Long> footfall = validatedTickets(today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                    .stream()
                    .collect(Collectors.groupingBy(event -> payload(event, "stationId", "UNKNOWN"), LinkedHashMap::new, Collectors.counting()));

            save("FREQ_STATION_FOOTFALL", "FREQ_06", "DAY", today.toString(),
                    footfall.values().stream().mapToLong(Long::longValue).sum(), new LinkedHashMap<>(footfall));
        } catch (Exception ex) {
            log.error("Failed to compute FREQ_06 station_footfall", ex);
        }
    }

    public void computeWeekendVsWeekdayRatio() {
        try {
            log.info("Computing FREQ_07 weekend_vs_weekday_ratio");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> tickets = validatedTickets(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());
            long weekend = tickets.stream().filter(event -> isWeekend(event.getTimestamp().getDayOfWeek())).count();
            long weekday = tickets.size() - weekend;
            double ratio = weekday == 0 ? 0 : weekend / (double) weekday;

            save("FREQ_WEEKEND_RATIO", "FREQ_07", "WEEK", weekPeriod(today), ratio,
                    Map.of("weekend_vs_weekday_ratio", ratio, "weekend", weekend, "weekday", weekday));
        } catch (Exception ex) {
            log.error("Failed to compute FREQ_07 weekend_vs_weekday_ratio", ex);
        }
    }

    private List<IncomingEvent> validatedTickets(LocalDateTime from, LocalDateTime to) {
        return eventRepository.findBySourceTypeAndTimestampBetween(SourceType.TICKETING, from, to)
                .stream()
                .filter(this::isValidatedTicket)
                .toList();
    }

    private boolean isValidatedTicket(IncomingEvent event) {
        if ("TICKET_VALIDATED".equalsIgnoreCase(event.getEventType())) {
            return true;
        }
        return "VALIDATED".equals(payload(event, "status", "").toUpperCase());
    }

    private Map<String, Long> hourlyDistribution(List<IncomingEvent> tickets) {
        Map<String, Long> counts = tickets.stream()
                .collect(Collectors.groupingBy(event -> String.valueOf(event.getTimestamp().getHour()), Collectors.counting()));
        Map<String, Long> distribution = new LinkedHashMap<>();
        IntStream.range(0, 24).forEach(hour -> distribution.put(String.valueOf(hour), counts.getOrDefault(String.valueOf(hour), 0L)));
        return distribution;
    }

    private void save(String statId, String displayId, String granularity, String period, double value, Map<String, Object> data) {
        snapshotRepository.save(StatSnapshot.builder()
                .snapshotType(SnapshotType.FREQUENCY)
                .statId(statId)
                .granularity(granularity)
                .period(period)
                .value(value)
                .metadata(Map.of("id", displayId, "data", data))
                .computedAt(LocalDateTime.now())
                .isPrediction(false)
                .build());
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

    private boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private String weekPeriod(LocalDate today) {
        return today.minusDays(6) + "/" + today;
    }
}
