package ma.sgitu.g8.alert;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.SnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ThresholdAlertService {

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${g5.notification.url}")
    private String g5Url;

    public void detect() {
        checkPunctuality();
        checkIncidentVolume();
        checkChurnRate();
        checkDailyRevenue();
        checkRepeatIncidentZones();
    }

    private void checkPunctuality() {
        snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("VEH_PUNCTUALITY")
                .filter(snapshot -> value(snapshot) < 80.0)
                .ifPresent(snapshot -> sendAlert(Map.of(
                        "notificationId", UUID.randomUUID().toString(),
                        "sourceService", "G8_ANALYTICS",
                        "eventType", "PUNCTUALITY_ALERT",
                        "channel", "EMAIL",
                        "priority", "HIGH",
                        "recipient", Map.of(
                                "userId", "op-01",
                                "email", "operateur@sgitu.ma"
                        ),
                        "metadata", Map.of(
                                "severity", "WARNING",
                                "targetAudience", "OPERATORS",
                                "value", value(snapshot),
                                "threshold", 80,
                                "lineId", "GLOBAL",
                                "period", YearMonth.now().toString(),
                                "statId", "VEH_02"
                        )
                )));
    }

    private void checkIncidentVolume() {
        snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("INC_TOTAL")
                .filter(snapshot -> value(snapshot) > 10)
                .ifPresent(snapshot -> sendAlert(Map.of(
                        "notificationId", UUID.randomUUID().toString(),
                        "sourceService", "G8_ANALYTICS",
                        "eventType", "HIGH_INCIDENT_VOLUME",
                        "channel", "EMAIL",
                        "priority", "HIGH",
                        "recipient", Map.of(
                                "userId", "sup-01",
                                "email", "superviseur@sgitu.ma"
                        ),
                        "metadata", Map.of(
                                "severity", "WARNING",
                                "targetAudience", "SUPERVISORS",
                                "value", value(snapshot),
                                "threshold", 10,
                                "date", LocalDate.now().toString(),
                                "statId", "INC_01"
                        )
                )));
    }

    private void checkChurnRate() {
        snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("SUB_CHURN")
                .filter(snapshot -> value(snapshot) > 15)
                .ifPresent(snapshot -> sendAlert(Map.of(
                        "notificationId", UUID.randomUUID().toString(),
                        "sourceService", "G8_ANALYTICS",
                        "eventType", "HIGH_CHURN_RATE",
                        "channel", "EMAIL",
                        "priority", "HIGH",
                        "recipient", Map.of(
                                "userId", "mgmt-01",
                                "email", "direction@sgitu.ma"
                        ),
                        "metadata", Map.of(
                                "severity", "WARNING",
                                "targetAudience", "MANAGEMENT",
                                "value", value(snapshot),
                                "threshold", 15,
                                "month", YearMonth.now().toString(),
                                "statId", "SUB_04"
                        )
                )));
    }

    private void checkDailyRevenue() {
        snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("REV_TOTAL")
                .ifPresent(todaySnapshot -> {
                    List<StatSnapshot> snapshots = snapshotRepository.findTop30ByStatIdOrderByComputedAtDesc("REV_TOTAL");
                    double average = snapshots.stream()
                            .mapToDouble(this::value)
                            .average()
                            .orElse(0);
                    double threshold = average * 0.70;
                    double todayValue = value(todaySnapshot);

                    if (average > 0 && todayValue < threshold) {
                        sendAlert(Map.of(
                                "notificationId", UUID.randomUUID().toString(),
                                "sourceService", "G8_ANALYTICS",
                                "eventType", "LOW_DAILY_REVENUE",
                                "channel", "EMAIL",
                                "priority", "HIGH",
                                "recipient", Map.of(
                                        "userId", "mgmt-01",
                                        "email", "direction@sgitu.ma"
                                ),
                                "metadata", Map.of(
                                        "severity", "WARNING",
                                        "targetAudience", "MANAGEMENT",
                                        "value", todayValue,
                                        "threshold", threshold,
                                        "date", LocalDate.now().toString(),
                                        "statId", "REV_01"
                                )
                        ));
                    }
                });
    }

    private void checkRepeatIncidentZones() {
        snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("INC_REPEAT_ZONES")
                .filter(snapshot -> value(snapshot) > 0)
                .ifPresent(snapshot -> sendAlert(Map.of(
                        "notificationId", UUID.randomUUID().toString(),
                        "sourceService", "G8_ANALYTICS",
                        "eventType", "INCIDENT_ZONE_RISK",
                        "channel", "EMAIL",
                        "priority", "HIGH",
                        "recipient", Map.of(
                                "userId", "op-01",
                                "email", "operateur@sgitu.ma"
                        ),
                        "metadata", Map.of(
                                "severity", "CRITICAL",
                                "targetAudience", "OPERATORS",
                                "value", value(snapshot),
                                "threshold", 3,
                                "zoneId", "GLOBAL",
                                "period", YearMonth.now().toString(),
                                "statId", "INC_05"
                        )
                )));
    }

    private void sendAlert(Map<String, Object> payload) {
        try {
            restTemplate.postForObject(g5Url, payload, Void.class);
            log.info("Alert sent to G5: {}", payload.get("eventType"));
        } catch (Exception ex) {
            log.error("Failed to send alert to G5", ex);
        }
    }

    private double value(StatSnapshot snapshot) {
        return snapshot.getValue() == null ? 0 : snapshot.getValue();
    }
}
