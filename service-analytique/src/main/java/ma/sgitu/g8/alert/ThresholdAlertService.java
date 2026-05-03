package ma.sgitu.g8.alert;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.SnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

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
                        "source", "G8_ANALYTICS",
                        "eventType", "PUNCTUALITY_ALERT",
                        "severity", "WARNING",
                        "notificationType", "EMAIL, PUSH",
                        "targetAudience", "OPERATORS",
                        "subject", "[SGITU] Taux de ponctualité critique",
                        "body", "Le taux de ponctualité est tombé à " + value(snapshot) + "% (seuil : 80%)",
                        "metadata", Map.of(
                                "statId", "VEH_02",
                                "value", value(snapshot),
                                "threshold", 80,
                                "period", "today"
                        )
                )));
    }

    private void checkIncidentVolume() {
        snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("INC_TOTAL")
                .filter(snapshot -> value(snapshot) > 10)
                .ifPresent(snapshot -> sendAlert(Map.of(
                        "source", "G8_ANALYTICS",
                        "eventType", "HIGH_INCIDENT_VOLUME",
                        "severity", "WARNING",
                        "notificationType", "EMAIL, SMS",
                        "targetAudience", "SUPERVISORS",
                        "subject", "[SGITU] Nombre d'incidents élevé",
                        "body", "Incidents aujourd'hui : " + value(snapshot) + " (seuil : 10)",
                        "metadata", Map.of(
                                "statId", "INC_01",
                                "value", value(snapshot),
                                "threshold", 10,
                                "period", "today"
                        )
                )));
    }

    private void checkChurnRate() {
        snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("SUB_CHURN")
                .filter(snapshot -> value(snapshot) > 15)
                .ifPresent(snapshot -> sendAlert(Map.of(
                        "source", "G8_ANALYTICS",
                        "eventType", "HIGH_CHURN_RATE",
                        "severity", "WARNING",
                        "notificationType", "EMAIL",
                        "targetAudience", "MANAGEMENT",
                        "subject", "[SGITU] Taux d'attrition élevé",
                        "body", "Taux de churn : " + value(snapshot) + "% (seuil : 15%)",
                        "metadata", Map.of(
                                "statId", "SUB_04",
                                "value", value(snapshot),
                                "threshold", 15,
                                "period", "this_month"
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
                                "source", "G8_ANALYTICS",
                                "eventType", "LOW_DAILY_REVENUE",
                                "severity", "WARNING",
                                "notificationType", "EMAIL",
                                "targetAudience", "MANAGEMENT",
                                "subject", "[SGITU] Revenu journalier bas",
                                "body", "Revenu du jour inférieur à 70% de la moyenne",
                                "metadata", Map.of(
                                        "statId", "REV_01",
                                        "value", todayValue,
                                        "threshold", threshold,
                                        "period", "today"
                                )
                        ));
                    }
                });
    }

    private void checkRepeatIncidentZones() {
        snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("INC_REPEAT_ZONES")
                .filter(snapshot -> value(snapshot) > 0)
                .ifPresent(snapshot -> sendAlert(Map.of(
                        "source", "G8_ANALYTICS",
                        "eventType", "INCIDENT_ZONE_RISK",
                        "severity", "CRITICAL",
                        "notificationType", "EMAIL, SMS, PUSH",
                        "targetAudience", "OPERATORS, SUPERVISORS",
                        "subject", "[SGITU] Zone à risque détectée",
                        "body", value(snapshot) + " zone(s) avec incidents répétés détectée(s)",
                        "metadata", Map.of(
                                "statId", "INC_05",
                                "value", value(snapshot),
                                "threshold", 3,
                                "period", "this_month"
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
