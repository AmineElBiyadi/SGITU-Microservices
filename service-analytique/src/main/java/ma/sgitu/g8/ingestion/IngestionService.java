package ma.sgitu.g8.ingestion;

import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final EventRepository eventRepository;

    public BatchIngestionResponse ingest(List<Map<String, Object>> rawEvents, SourceType sourceType) {
        if (rawEvents == null || rawEvents.isEmpty()) {
            return BatchIngestionResponse.builder()
                    .totalReceived(rawEvents == null ? 0 : rawEvents.size())
                    .totalAccepted(0)
                    .totalRejected(rawEvents == null ? 0 : rawEvents.size())
                    .rejectedReasons(List.of("Request body must contain at least one event."))
                    .status("REJECTED")
                    .build();
        }

        List<IncomingEvent> acceptedEvents = new ArrayList<>();
        List<String> rejectedReasons = new ArrayList<>();

        for (int index = 0; index < rawEvents.size(); index++) {
            Map<String, Object> rawEvent = rawEvents.get(index);
            String validationError = validate(rawEvent);

            if (validationError != null) {
                rejectedReasons.add("Event " + index + ": " + validationError);
                continue;
            }

            acceptedEvents.add(mapToEvent(rawEvent, sourceType));
        }

        if (!acceptedEvents.isEmpty()) {
            eventRepository.saveAll(acceptedEvents);
        }

        int totalReceived = rawEvents.size();
        int totalAccepted = acceptedEvents.size();
        int totalRejected = rejectedReasons.size();

        return BatchIngestionResponse.builder()
                .totalReceived(totalReceived)
                .totalAccepted(totalAccepted)
                .totalRejected(totalRejected)
                .rejectedReasons(rejectedReasons)
                .status(resolveBatchStatus(totalAccepted, totalRejected))
                .build();
    }

    private String resolveBatchStatus(int totalAccepted, int totalRejected) {
        if (totalAccepted == 0) {
            return "REJECTED";
        }
        if (totalRejected == 0) {
            return "SUCCESS";
        }
        return "PARTIAL";
    }

    private String validate(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return "Payload must not be null or empty.";
        }

        Object timestampValue = raw.get("timestamp");
        if (!(timestampValue instanceof String timestamp) || timestamp.isBlank()) {
            return "Missing required timestamp.";
        }

        try {
            OffsetDateTime parsedTimestamp = OffsetDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
            if (parsedTimestamp.isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5))) {
                return "Timestamp cannot be more than 5 minutes in the future.";
            }
        } catch (DateTimeParseException ex) {
            return "Timestamp must be a valid ISO 8601 value.";
        }

        return null;
    }

    private IncomingEvent mapToEvent(Map<String, Object> raw, SourceType sourceType) {
        IncomingEvent event = new IncomingEvent();
        event.setSourceType(sourceType);
        event.setSourceId(resolveSourceId(raw, sourceType));
        event.setEventType(resolveEventType(raw, sourceType));
        event.setPayload(raw);

        Object timestamp = raw.get("timestamp");
        event.setTimestamp(timestamp instanceof String ts
                ? OffsetDateTime.parse(ts, TIMESTAMP_FORMATTER).toLocalDateTime()
                : LocalDateTime.now());

        event.setReceivedAt(LocalDateTime.now());
        event.setLineId(readString(raw, "line"));
        event.setZoneId(readString(raw, "zone"));
        event.setProcessed(false);
        return event;
    }

    private String resolveSourceId(Map<String, Object> raw, SourceType sourceType) {
        return switch (sourceType) {
            case TICKETING -> getOrGenerate(raw, "userId");
            case SUBSCRIPTION -> getOrGenerate(raw, "userId");
            case PAYMENT -> getOrGenerate(raw, "transactionId");
            case VEHICLE -> getOrGenerate(raw, "vehicleId");
            case INCIDENT -> getOrGenerate(raw, "incidentId");
            case USER -> getOrGenerate(raw, "userId");
        };
    }

    private String resolveEventType(Map<String, Object> raw, SourceType sourceType) {
        return switch (sourceType) {
            case TICKETING -> switch (normalize(raw, "status")) {
                case "validated" -> "TICKET_VALIDATED";
                case "expired" -> "TICKET_EXPIRED";
                default -> "TICKET_EVENT";
            };
            case SUBSCRIPTION -> switch (normalize(raw, "action")) {
                case "created" -> "SUBSCRIPTION_CREATED";
                case "renewed" -> "SUBSCRIPTION_RENEWED";
                case "cancelled" -> "SUBSCRIPTION_CANCELLED";
                default -> "SUBSCRIPTION_EVENT";
            };
            case PAYMENT -> switch (normalize(raw, "status")) {
                case "completed" -> "PAYMENT_COMPLETED";
                case "failed" -> "PAYMENT_FAILED";
                default -> "PAYMENT_EVENT";
            };
            case VEHICLE -> switch (normalize(raw, "status")) {
                case "in_service" -> "VEHICLE_IN_SERVICE";
                case "out_of_service" -> "VEHICLE_OUT_OF_SERVICE";
                default -> "VEHICLE_EVENT";
            };
            case INCIDENT -> switch (normalize(raw, "type")) {
                case "delay" -> "INCIDENT_DELAY";
                case "breakdown" -> "INCIDENT_BREAKDOWN";
                case "accident" -> "INCIDENT_ACCIDENT";
                default -> "INCIDENT_REPORTED";
            };
            case USER -> switch (normalize(raw, "action")) {
                case "active" -> "USER_ACTIVE";
                case "inactive" -> "USER_INACTIVE";
                default -> "USER_EVENT";
            };
        };
    }

    private String getOrGenerate(Map<String, Object> raw, String key) {
        String value = readString(raw, key);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }

    private String readString(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String normalize(Map<String, Object> raw, String key) {
        String value = readString(raw, key);
        return value == null ? "" : value.trim().toLowerCase();
    }
}
