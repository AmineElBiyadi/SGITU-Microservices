package ma.sgitu.g8.ingestion;

import ma.sgitu.g8.model.SourceType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SourceEventNormalizer {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public Map<String, Object> normalize(Map<String, Object> raw, SourceType sourceType) {
        if (raw == null) {
            return null;
        }

        Map<String, Object> event = new LinkedHashMap<>(raw);
        normalizeCommonTimestamp(event);

        switch (sourceType) {
            case TICKETING -> normalizeTicket(event);
            case PAYMENT -> normalizePayment(event);
            case VEHICLE -> normalizeVehicle(event);
            case INCIDENT -> normalizeIncident(event);
            case SUBSCRIPTION -> normalizeSubscription(event);
            case USER -> normalizeUser(event);
        }

        return event;
    }

    private void normalizeTicket(Map<String, Object> event) {
        copyFirstPresent(event, "timestamp", "redeemedAt", "expiredAt", "issuedAt", "createdAt");
        copyFirstPresent(event, "scanType", "methodeValidation", "validationMethod");
        normalizeTimestampValue(event, "timestamp");

        String eventType = normalizedString(event.get("eventType"));
        if (isBlank(event.get("status"))) {
            if ("TICKET_VALIDATED".equals(eventType)) {
                event.put("status", "validated");
            } else if ("TICKET_EXPIRED".equals(eventType)) {
                event.put("status", "expired");
            }
        }
    }

    private void normalizePayment(Map<String, Object> event) {
        copyFirstPresent(event, "timestamp", "completedAt", "paidAt", "createdAt");
        copyFirstPresent(event, "transactionId", "transactionToken", "paymentId", "id");
        copyFirstPresent(event, "amount", "totalAmount", "total_amount");
        copyFirstPresent(event, "method", "paymentMethod");
        normalizeTimestampValue(event, "timestamp");

        String status = normalizedString(event.get("status"));
        if ("SUCCESS".equals(status)) {
            event.put("status", "completed");
        } else if ("FAILED".equals(status) || "FAILURE".equals(status)) {
            event.put("status", "failed");
        }
    }

    private void normalizeVehicle(Map<String, Object> event) {
        copyFirstPresent(event, "timestamp", "dateDetection", "createdAt");
        copyFirstPresent(event, "vehicleId", "vehiculeId");
        copyFirstPresent(event, "line", "lineId", "ligneId", "ligne", "ligneTransport");
        copyFirstPresent(event, "speed", "vitesse", "avgSpeed");
        copyFirstPresent(event, "delayMinutes", "retardMinutes", "retard");
        normalizeTimestampValue(event, "timestamp");

        String status = normalizedString(event.get("status"));
        if ("EN_SERVICE".equals(status) || "IN_SERVICE".equals(status)) {
            event.put("status", "in_service");
        } else if ("HORS_SERVICE".equals(status) || "OUT_OF_SERVICE".equals(status)) {
            event.put("status", "out_of_service");
        }

        if (isBlank(event.get("line"))) {
            event.put("line", "UNKNOWN");
        }
    }

    private void normalizeIncident(Map<String, Object> event) {
        copyFirstPresent(event, "timestamp", "dateIncident", "dateSignalement", "dateDetection", "createdAt");
        copyFirstPresent(event, "incidentId", "reference", "id");
        copyFirstPresent(event, "line", "ligneTransport", "lineId", "ligneId");
        normalizeTimestampValue(event, "timestamp");
        normalizeIncidentType(event);
        normalizeIncidentSeverity(event);
        normalizeResolutionMinutes(event);
    }

    private void normalizeSubscription(Map<String, Object> event) {
        copyFirstPresent(event, "timestamp", "createdAt", "updatedAt");
        copyFirstPresent(event, "subscriptionId", "abonnementId", "id");
        copyFirstPresent(event, "planType", "subscriptionType", "typeAbonnement", "planName");
        normalizeTimestampValue(event, "timestamp");

        String action = normalizedString(event.get("action"));
        String type = normalizedString(event.get("type"));
        String eventType = normalizedString(event.get("eventType"));
        String candidate = !action.isBlank() ? action : (!type.isBlank() ? type : eventType);

        switch (candidate) {
            case "SOUSCRIPTION_INITIALE", "SUBSCRIPTION_CREATED", "CREATED" -> event.put("action", "created");
            case "RENOUVELLEMENT", "SUBSCRIPTION_RENEWED", "RENEWED" -> event.put("action", "renewed");
            case "ANNULATION_CONFIRMEE", "SUBSCRIPTION_CANCELLED", "CANCELLED" -> event.put("action", "cancelled");
            default -> {
                if (!action.isBlank()) {
                    event.put("action", action.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    private void normalizeUser(Map<String, Object> event) {
        copyFirstPresent(event, "timestamp", "createdAt", "updatedAt");
        normalizeTimestampValue(event, "timestamp");
    }

    private void normalizeIncidentType(Map<String, Object> event) {
        String type = normalizedString(event.get("type"));
        switch (type) {
            case "RETARD", "DELAY" -> event.put("type", "delay");
            case "PANNE", "PANNE_VEHICULE", "BREAKDOWN" -> event.put("type", "breakdown");
            case "ACCIDENT" -> event.put("type", "accident");
            default -> {
                if (!type.isBlank()) {
                    event.put("type", type.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    private void normalizeIncidentSeverity(Map<String, Object> event) {
        copyFirstPresent(event, "severity", "gravite", "severite");
        String severity = normalizedString(event.get("severity"));
        switch (severity) {
            case "FAIBLE", "LOW" -> event.put("severity", "LOW");
            case "MOYEN", "MOYENNE", "MEDIUM" -> event.put("severity", "MEDIUM");
            case "ELEVE", "ELEVEE", "HAUTE", "HIGH" -> event.put("severity", "HIGH");
            case "CRITIQUE", "CRITICAL" -> event.put("severity", "CRITICAL");
            default -> {
                if (!severity.isBlank()) {
                    event.put("severity", severity);
                }
            }
        }
    }

    private void normalizeResolutionMinutes(Map<String, Object> event) {
        if (!isBlank(event.get("resolutionMinutes"))) {
            return;
        }

        OffsetDateTime start = parseDateTime(firstPresentValue(event, "dateIncident", "dateSignalement", "timestamp"));
        OffsetDateTime end = parseDateTime(firstPresentValue(event, "dateResolution"));
        if (start != null && end != null && !end.isBefore(start)) {
            event.put("resolutionMinutes", Duration.between(start, end).toMinutes());
        }
    }

    private void normalizeCommonTimestamp(Map<String, Object> event) {
        normalizeTimestampValue(event, "timestamp");
    }

    private void normalizeTimestampValue(Map<String, Object> event, String key) {
        Object value = event.get(key);
        if (isBlank(value)) {
            return;
        }
        OffsetDateTime dateTime = parseDateTime(value);
        if (dateTime != null) {
            event.put(key, dateTime.format(ISO_FORMATTER));
        }
    }

    private OffsetDateTime parseDateTime(Object value) {
        if (isBlank(value)) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atOffset(ZoneOffset.UTC);
        }

        String text = String.valueOf(value).trim();
        for (String candidate : List.of(text, text + "Z")) {
            try {
                return OffsetDateTime.parse(candidate, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                // Try the next shape.
            }
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME).atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private void copyFirstPresent(Map<String, Object> event, String target, String... aliases) {
        if (!isBlank(event.get(target))) {
            return;
        }
        Object value = firstPresentValue(event, aliases);
        if (!isBlank(value)) {
            event.put(target, value);
        }
    }

    private Object firstPresentValue(Map<String, Object> event, String... keys) {
        for (String key : keys) {
            Object value = event.get(key);
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    private String normalizedString(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }
}
