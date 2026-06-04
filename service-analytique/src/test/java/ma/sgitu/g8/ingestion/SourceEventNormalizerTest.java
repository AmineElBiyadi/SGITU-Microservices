package ma.sgitu.g8.ingestion;

import ma.sgitu.g8.model.SourceType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SourceEventNormalizerTest {

    private final SourceEventNormalizer normalizer = new SourceEventNormalizer();

    @Test
    void ticketValidatedEvent_isAdaptedToG8Contract() {
        Map<String, Object> normalized = normalizer.normalize(Map.of(
                "schemaVersion", 1,
                "eventType", "TICKET_VALIDATED",
                "ticketId", "t-01",
                "userId", "u-01",
                "methodeValidation", "QR",
                "redeemedAt", "2026-06-02T10:15:00Z"
        ), SourceType.TICKETING);

        assertThat(normalized)
                .containsEntry("status", "validated")
                .containsEntry("timestamp", "2026-06-02T10:15:00Z")
                .containsEntry("scanType", "QR");
    }

    @Test
    void vehicleStatusWithoutLine_getsUnknownLineFallback() {
        Map<String, Object> normalized = normalizer.normalize(Map.of(
                "schemaVersion", 1,
                "timestamp", "2026-06-02T10:15:00Z",
                "vehicleId", "BUS-01",
                "status", "EN_SERVICE",
                "vitesse", 44.5,
                "retardMinutes", 8
        ), SourceType.VEHICLE);

        assertThat(normalized)
                .containsEntry("status", "in_service")
                .containsEntry("line", "UNKNOWN")
                .containsEntry("speed", 44.5)
                .containsEntry("delayMinutes", 8);
    }

    @Test
    void frenchIncidentEvent_isAdaptedToG8Contract() {
        Map<String, Object> normalized = normalizer.normalize(Map.of(
                "schemaVersion", 1,
                "reference", "INC-LEGACY-01",
                "type", "PANNE_VEHICULE",
                "gravite", "CRITIQUE",
                "ligneTransport", "L2",
                "latitude", 33.5731,
                "longitude", -7.5898,
                "dateIncident", "2026-06-02T10:00:00",
                "dateResolution", "2026-06-02T10:30:00"
        ), SourceType.INCIDENT);

        assertThat(normalized)
                .containsEntry("incidentId", "INC-LEGACY-01")
                .containsEntry("type", "breakdown")
                .containsEntry("severity", "CRITICAL")
                .containsEntry("line", "L2")
                .containsEntry("timestamp", "2026-06-02T10:00:00Z")
                .containsEntry("resolutionMinutes", 30L);
    }

    @Test
    void paymentSuccessEvent_isAdaptedToG8Contract() {
        Map<String, Object> normalized = normalizer.normalize(Map.of(
                "schemaVersion", 1,
                "transactionToken", "TX-01",
                "amount", 25.0,
                "status", "SUCCESS",
                "paymentMethod", "CARD",
                "createdAt", "2026-06-02T10:15:00"
        ), SourceType.PAYMENT);

        assertThat(normalized)
                .containsEntry("transactionId", "TX-01")
                .containsEntry("status", "completed")
                .containsEntry("method", "CARD")
                .containsEntry("timestamp", "2026-06-02T10:15:00Z");
    }

    @Test
    void normalizerDoesNotAddSchemaVersion() {
        Map<String, Object> normalized = normalizer.normalize(Map.of(
                "timestamp", "2026-06-02T10:15:00Z",
                "userId", "u-01",
                "status", "validated"
        ), SourceType.TICKETING);

        assertThat(normalized).doesNotContainKey("schemaVersion");
    }
}
