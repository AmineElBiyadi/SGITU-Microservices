package ma.sgitu.g8.ingestion;

import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import ma.sgitu.g8.geo.ZoneResolver;
import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.schema.SchemaVersionValidator;
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
    private static final int MAX_BATCH_SIZE = 1000;

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

        if (rawEvents.size() > MAX_BATCH_SIZE) {
            return BatchIngestionResponse.builder()
                    .totalReceived(rawEvents.size())
                    .totalAccepted(0)
                    .totalRejected(rawEvents.size())
                    .rejectedReasons(List.of("Batch size exceeds maximum allowed (1000)"))
                    .status("REJECTED")
                    .build();
        }

        List<IncomingEvent> acceptedEvents = new ArrayList<>();
        List<String> rejectedReasons = new ArrayList<>();

        for (int index = 0; index < rawEvents.size(); index++) {
            Map<String, Object> rawEvent = rawEvents.get(index);
            String validationError = validate(rawEvent, sourceType);

            if (validationError != null) {
                rejectedReasons.add("Event at index " + index + ": " + validationError);
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

    private String validate(Map<String, Object> raw, SourceType sourceType) {
        if (raw == null || raw.isEmpty()) {
            return "Payload must not be null or empty.";
        }

        String schemaError = SchemaVersionValidator.validate(raw, IncomingEvent.CURRENT_SCHEMA_VERSION);
        if (schemaError != null) {
            return schemaError;
        }

        String requiredFieldError = validateRequiredFields(raw, sourceType);
        if (requiredFieldError != null) {
            return requiredFieldError;
        }

        String timestampError = validateTimestamp(raw);
        if (timestampError != null) {
            return timestampError;
        }

        String numericError = validateNumericFields(raw);
        if (numericError != null) {
            return numericError;
        }

        String enumError = validateAllowedValues(raw, sourceType);
        if (enumError != null) {
            return enumError;
        }

        return null;
    }

    private String validateRequiredFields(Map<String, Object> raw, SourceType sourceType) {
        return switch (sourceType) {
            case TICKETING -> firstMissing(raw, "timestamp", "userId", "status");
            case PAYMENT -> firstMissing(raw, "timestamp", "transactionId", "amount", "status");
            case VEHICLE -> firstMissing(raw, "timestamp", "vehicleId", "status", "line");
            case INCIDENT -> firstMissing(raw, "timestamp", "incidentId", "type", "severity", "latitude", "longitude");
            case SUBSCRIPTION -> firstMissing(raw, "timestamp", "userId", "action");
            case USER -> firstMissing(raw, "timestamp", "userId", "action");
        };
    }

    private String firstMissing(Map<String, Object> raw, String... fields) {
        for (String field : fields) {
            Object value = raw.get(field);
            if (value == null || String.valueOf(value).isBlank()) {
                return "Missing required field: " + field;
            }
        }
        return null;
    }

    private String validateTimestamp(Map<String, Object> raw) {
        Object timestampValue = raw.get("timestamp");
        if (!(timestampValue instanceof String timestamp) || timestamp.isBlank()) {
            return "Missing required field: timestamp";
        }

        try {
            OffsetDateTime parsedTimestamp = OffsetDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
            if (parsedTimestamp.isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5))) {
                return "Timestamp cannot be more than 5 minutes in the future.";
            }
        } catch (DateTimeParseException ex) {
            return "Invalid timestamp format";
        }

        return null;
    }

    private String validateNumericFields(Map<String, Object> raw) {
        for (String field : List.of("amount", "speed", "delayMinutes", "resolutionMinutes")) {
            String error = validateNonNegativeField(raw, field);
            if (error != null) {
                return error;
            }
        }
        return validateCoordinateFields(raw);
    }

    private String validateNonNegativeField(Map<String, Object> raw, String field) {
        Object value = raw.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        Double numericValue = readNumber(value);
        if (numericValue == null) {
            return "Field " + field + " must be numeric";
        }
        if (numericValue < 0) {
            return "Field " + field + " must be >= 0, got " + value;
        }
        return null;
    }

    private String validateCoordinateFields(Map<String, Object> raw) {
        Double latitude = readCoordinate(raw, "latitude", "lat");
        Double longitude = readCoordinate(raw, "longitude", "lon", "lng");
        if (latitude == null && longitude == null) {
            return null;
        }
        if (latitude == null || longitude == null) {
            return "Both latitude and longitude are required when providing GPS coordinates.";
        }
        if (latitude < -90 || latitude > 90) {
            return "Field latitude must be between -90 and 90, got " + latitude;
        }
        if (longitude < -180 || longitude > 180) {
            return "Field longitude must be between -180 and 180, got " + longitude;
        }
        return null;
    }

    private Double readNumber(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String validateAllowedValues(Map<String, Object> raw, SourceType sourceType) {
        return switch (sourceType) {
            case TICKETING -> validateAllowed(raw, "status", "validated", "expired");
            case PAYMENT -> validateAllowed(raw, "status", "completed", "failed");
            case VEHICLE -> validateAllowed(raw, "status", "in_service", "out_of_service");
            case INCIDENT -> validateAllowed(raw, "severity", "LOW", "MEDIUM", "HIGH", "CRITICAL");
            case SUBSCRIPTION -> validateAllowed(raw, "action", "created", "renewed", "cancelled");
            case USER -> validateAllowed(raw, "action", "active", "inactive");
        };
    }

    private String validateAllowed(Map<String, Object> raw, String field, String... allowedValues) {
        String value = readString(raw, field);
        for (String allowed : allowedValues) {
            if (allowed.equalsIgnoreCase(value)) {
                return null;
            }
        }
        return "Invalid value '" + value + "' for field '" + field + "'";
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

        event.setSchemaVersion(IncomingEvent.CURRENT_SCHEMA_VERSION);
        event.setReceivedAt(LocalDateTime.now());
        event.setLineId(readString(raw, "line"));
        event.setZoneId(resolveIncidentZoneId(raw, sourceType));
        event.setProcessed(false);
        return event;
    }

    private String resolveIncidentZoneId(Map<String, Object> raw, SourceType sourceType) {
        if (sourceType != SourceType.INCIDENT) {
            return readString(raw, "zone");
        }
        Double latitude = readCoordinate(raw, "latitude", "lat");
        Double longitude = readCoordinate(raw, "longitude", "lon", "lng");
        if (latitude == null || longitude == null) {
            return ZoneResolver.UNKNOWN_ZONE;
        }
        return ZoneResolver.resolve(latitude, longitude);
    }

    private Double readCoordinate(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            Object value = raw.get(key);
            if (value == null || String.valueOf(value).isBlank()) {
                continue;
            }
            Double numeric = readNumber(value);
            if (numeric != null) {
                return numeric;
            }
        }
        return null;
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
