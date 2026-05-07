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
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubscriptionAggregation {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SnapshotService snapshotService;

    public void compute() {
        computeActiveSubscriptions();
        computeNewSubscriptions();
        computeRenewalRate();
        computeChurnRate();
        computeSubscriptionTypeDistribution();
    }

    public void computeActiveSubscriptions() {
        try {
            log.info("Computing SUB_01 active_subscriptions");
            List<IncomingEvent> subscriptions = eventRepository.findBySourceTypeAndProcessedFalse(SourceType.SUBSCRIPTION);
            long active = subscriptions.stream()
                    .filter(event -> "SUBSCRIPTION_CREATED".equals(event.getEventType())
                            || "SUBSCRIPTION_RENEWED".equals(event.getEventType()))
                    .count();

            save("SUB_ACTIVE", "SUB_01", "REAL_TIME", "now", active,
                    Map.of("active_subscriptions", active));
        } catch (Exception ex) {
            log.error("Failed to compute SUB_01 active_subscriptions", ex);
        }
    }

    public void computeNewSubscriptions() {
        try {
            log.info("Computing SUB_02 new_subscriptions");
            LocalDate today = LocalDate.now();
            long countDay = subscriptions(today.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                    .filter(event -> "SUBSCRIPTION_CREATED".equals(event.getEventType()))
                    .count();
            long countWeek = subscriptions(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                    .filter(event -> "SUBSCRIPTION_CREATED".equals(event.getEventType()))
                    .count();

            save("SUB_NEW", "SUB_02", "WEEK", weekPeriod(today), countWeek,
                    Map.of("today", countDay, "this_week", countWeek));
        } catch (Exception ex) {
            log.error("Failed to compute SUB_02 new_subscriptions", ex);
        }
    }

    public void computeRenewalRate() {
        try {
            log.info("Computing SUB_03 renewal_rate");
            LocalDate today = LocalDate.now();
            YearMonth month = YearMonth.from(today);
            List<IncomingEvent> subscriptions = subscriptions(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
            long renewals = subscriptions.stream()
                    .filter(event -> "SUBSCRIPTION_RENEWED".equals(event.getEventType()))
                    .count();
            double rate = subscriptions.isEmpty() ? 0 : renewals * 100.0 / subscriptions.size();

            save("SUB_RENEWAL_RATE", "SUB_03", "MONTH", month.toString(), rate,
                    Map.of("renewal_rate", rate, "renewals", renewals, "total", subscriptions.size()));
        } catch (Exception ex) {
            log.error("Failed to compute SUB_03 renewal_rate", ex);
        }
    }

    public void computeChurnRate() {
        try {
            log.info("Computing SUB_04 churn_rate");
            LocalDate today = LocalDate.now();
            YearMonth month = YearMonth.from(today);
            List<IncomingEvent> subscriptions = subscriptions(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
            long cancellations = subscriptions.stream()
                    .filter(event -> "SUBSCRIPTION_CANCELLED".equals(event.getEventType()))
                    .count();
            double rate = subscriptions.isEmpty() ? 0 : cancellations * 100.0 / subscriptions.size();

            save("SUB_CHURN", "SUB_04", "MONTH", month.toString(), rate,
                    Map.of("churn_rate", rate, "cancellations", cancellations, "total", subscriptions.size()));
        } catch (Exception ex) {
            log.error("Failed to compute SUB_04 churn_rate", ex);
        }
    }

    public void computeSubscriptionTypeDistribution() {
        try {
            log.info("Computing SUB_05 subscription_type_distribution");
            LocalDate today = LocalDate.now();
            YearMonth month = YearMonth.from(today);
            List<IncomingEvent> subscriptions = subscriptions(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
            Map<String, Long> counts = subscriptions.stream()
                    .collect(Collectors.groupingBy(
                            event -> payload(event, "planType", "OTHER").toUpperCase(),
                            LinkedHashMap::new,
                            Collectors.counting()
                    ));
            double total = subscriptions.size();
            Map<String, Double> distribution = new LinkedHashMap<>();
            counts.forEach((type, count) -> distribution.put(type, total == 0 ? 0 : count * 100.0 / total));

            save("SUB_TYPE_DIST", "SUB_05", "MONTH", month.toString(), total,
                    Map.of("subscription_type_distribution", distribution, "total_subscriptions", total));
        } catch (Exception ex) {
            log.error("Failed to compute SUB_05 subscription_type_distribution", ex);
        }
    }

    private List<IncomingEvent> subscriptions(LocalDateTime from, LocalDateTime to) {
        return eventRepository.findBySourceTypeAndTimestampBetween(SourceType.SUBSCRIPTION, from, to);
    }

    private void save(String statId, String displayId, String granularity, String period, double value, Map<String, Object> data) {
        StatSnapshot snapshot = StatSnapshot.builder()
                .snapshotType(SnapshotType.SUBSCRIPTIONS)
                .statId(statId)
                .granularity(granularity)
                .period(period)
                .value(value)
                .metadata(Map.of("id", displayId, "data", data))
                .isPrediction(false)
                .build();
        snapshotService.upsert(statId, SnapshotType.SUBSCRIPTIONS, snapshot);
    }

    private String action(IncomingEvent event) {
        return payload(event, "action", "").toUpperCase();
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
