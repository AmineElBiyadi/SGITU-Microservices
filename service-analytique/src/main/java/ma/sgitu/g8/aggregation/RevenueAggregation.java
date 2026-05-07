package ma.sgitu.g8.aggregation;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.SnapshotRepository;
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
import java.util.stream.IntStream;

@Slf4j
@Service
public class RevenueAggregation {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Autowired
    private SnapshotService snapshotService;

    public void compute() {
        computeTotalRevenue();
        computeRevenueByTicketType();
        computeAvgRevenuePerPassenger();
        computePaymentMethodBreakdown();
        computeRevenueTrend();
    }

    public void computeTotalRevenue() {
        try {
            log.info("Computing REV_01 total_revenue");
            LocalDate today = LocalDate.now();
            List<IncomingEvent> payments = successfulPayments(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            double total = payments.stream().mapToDouble(this::amount).sum();

            save("REV_TOTAL", "REV_01", "DAY", today.toString(), total,
                    Map.of("total_revenue", total, "date", today.toString()));
        } catch (Exception ex) {
            log.error("Failed to compute REV_01 total_revenue", ex);
        }
    }

    public void computeRevenueByTicketType() {
        try {
            log.info("Computing REV_02 revenue_by_ticket_type");
            LocalDate today = LocalDate.now();
            YearMonth month = YearMonth.from(today);
            Map<String, Double> byType = successfulPayments(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay())
                    .stream()
                    .collect(Collectors.groupingBy(
                            event -> payload(event, "method", "OTHER").toUpperCase(),
                            LinkedHashMap::new,
                            Collectors.summingDouble(this::amount)
                    ));

            save("REV_BY_TYPE", "REV_02", "MONTH", month.toString(),
                    byType.values().stream().mapToDouble(Double::doubleValue).sum(), new LinkedHashMap<>(byType));
        } catch (Exception ex) {
            log.error("Failed to compute REV_02 revenue_by_ticket_type", ex);
        }
    }

    public void computeAvgRevenuePerPassenger() {
        try {
            log.info("Computing REV_03 avg_revenue_per_passenger");
            LocalDate today = LocalDate.now();
            double revenue = snapshotValue("REV_TOTAL");
            double passengers = snapshotValue("FREQ_TOTAL_VALIDATIONS");
            double average = passengers == 0 ? 0 : revenue / passengers;

            save("REV_AVG_PER_PASSENGER", "REV_03", "WEEK", today.minusDays(6) + "/" + today, average,
                    Map.of("avg_revenue_per_passenger", average, "revenue", revenue, "passengers", passengers));
        } catch (Exception ex) {
            log.error("Failed to compute REV_03 avg_revenue_per_passenger", ex);
        }
    }

    public void computePaymentMethodBreakdown() {
        try {
            log.info("Computing REV_04 payment_method_breakdown");
            LocalDate today = LocalDate.now();
            YearMonth month = YearMonth.from(today);
            List<IncomingEvent> payments = payments(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
            Map<String, Long> counts = payments.stream()
                    .collect(Collectors.groupingBy(
                            event -> payload(event, "method", "UNKNOWN").toUpperCase(),
                            LinkedHashMap::new,
                            Collectors.counting()
                    ));
            double total = payments.size();
            Map<String, Double> breakdown = new LinkedHashMap<>();
            counts.forEach((method, count) -> breakdown.put(method, total == 0 ? 0 : count * 100.0 / total));

            save("REV_PAYMENT_METHOD", "REV_04", "MONTH", month.toString(), total,
                    Map.of("payment_method_breakdown", breakdown, "total_payments", total));
        } catch (Exception ex) {
            log.error("Failed to compute REV_04 payment_method_breakdown", ex);
        }
    }

    public void computeRevenueTrend() {
        try {
            log.info("Computing REV_05 revenue_trend");
            LocalDate today = LocalDate.now();
            LocalDate start = today.minusDays(29);
            Map<String, Double> totalsByDate = successfulPayments(start.atStartOfDay(), today.plusDays(1).atStartOfDay())
                    .stream()
                    .collect(Collectors.groupingBy(
                            event -> event.getTimestamp().toLocalDate().toString(),
                            Collectors.summingDouble(this::amount)
                    ));

            List<Map<String, Object>> trend = IntStream.range(0, 30)
                    .mapToObj(start::plusDays)
                    .map(date -> {
                        Map<String, Object> point = new LinkedHashMap<>();
                        point.put("date", date.toString());
                        point.put("amount", totalsByDate.getOrDefault(date.toString(), 0.0));
                        return point;
                    })
                    .toList();
            double total = trend.stream().mapToDouble(point -> (Double) point.get("amount")).sum();

            save("REV_TREND", "REV_05", "DAY", start + "/" + today, total,
                    Map.of("revenue_trend", trend));
        } catch (Exception ex) {
            log.error("Failed to compute REV_05 revenue_trend", ex);
        }
    }

    private List<IncomingEvent> successfulPayments(LocalDateTime from, LocalDateTime to) {
        return payments(from, to).stream()
                .filter(event -> "PAYMENT_COMPLETED".equals(event.getEventType()))
                .toList();
    }

    private List<IncomingEvent> payments(LocalDateTime from, LocalDateTime to) {
        return eventRepository.findBySourceTypeAndTimestampBetween(SourceType.PAYMENT, from, to);
    }

    private double snapshotValue(String statId) {
        return snapshotRepository.findFirstByStatIdOrderByComputedAtDesc(statId)
                .map(StatSnapshot::getValue)
                .orElse(0.0);
    }

    private void save(String statId, String displayId, String granularity, String period, double value, Map<String, Object> data) {
        StatSnapshot snapshot = StatSnapshot.builder()
                .snapshotType(SnapshotType.REVENUE)
                .statId(statId)
                .granularity(granularity)
                .period(period)
                .value(value)
                .metadata(Map.of("id", displayId, "data", data))
                .isPrediction(false)
                .build();
        snapshotService.upsert(statId, SnapshotType.REVENUE, snapshot);
    }

    private String status(IncomingEvent event) {
        return payload(event, "status", "").toUpperCase();
    }

    private String payload(IncomingEvent event, String key, String defaultValue) {
        Object value = event.getPayload() == null ? null : event.getPayload().get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private double amount(IncomingEvent event) {
        Object value = event.getPayload() == null ? null : event.getPayload().get("amount");
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
