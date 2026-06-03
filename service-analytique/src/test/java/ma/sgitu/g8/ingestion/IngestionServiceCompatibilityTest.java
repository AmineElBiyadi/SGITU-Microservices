package ma.sgitu.g8.ingestion;

import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngestionServiceCompatibilityTest {

    private EventRepository eventRepository;
    private IngestionService ingestionService;

    @BeforeEach
    void setup() {
        eventRepository = mock(EventRepository.class);
        when(eventRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        ingestionService = new IngestionService(eventRepository, new SourceEventNormalizer());
    }

    @Test
    void legacyTicketValidatedEvent_isAcceptedAndPersistedAsTicketValidated() {
        var response = ingestionService.ingest(List.of(Map.of(
                "schemaVersion", 1,
                "eventType", "TICKET_VALIDATED",
                "ticketId", "TCK-LEGACY-01",
                "userId", "USR-01",
                "methodeValidation", "QR",
                "redeemedAt", "2026-06-02T10:15:00Z"
        )), SourceType.TICKETING);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        IncomingEvent saved = savedEvent();
        assertThat(saved.getSourceType()).isEqualTo(SourceType.TICKETING);
        assertThat(saved.getSourceId()).isEqualTo("USR-01");
        assertThat(saved.getEventType()).isEqualTo("TICKET_VALIDATED");
        assertThat(saved.getPayload())
                .containsEntry("status", "validated")
                .containsEntry("scanType", "QR");
    }

    @Test
    void legacyVehicleStatusEvent_isAcceptedWithUnknownLineFallback() {
        var response = ingestionService.ingest(List.of(Map.of(
                "schemaVersion", 1,
                "timestamp", "2026-06-02T10:15:00Z",
                "vehicleId", "BUS-01",
                "status", "EN_SERVICE",
                "vitesse", 44.5,
                "retardMinutes", 8
        )), SourceType.VEHICLE);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        IncomingEvent saved = savedEvent();
        assertThat(saved.getSourceType()).isEqualTo(SourceType.VEHICLE);
        assertThat(saved.getSourceId()).isEqualTo("BUS-01");
        assertThat(saved.getEventType()).isEqualTo("VEHICLE_IN_SERVICE");
        assertThat(saved.getLineId()).isEqualTo("UNKNOWN");
        assertThat(saved.getPayload())
                .containsEntry("status", "in_service")
                .containsEntry("line", "UNKNOWN")
                .containsEntry("speed", 44.5)
                .containsEntry("delayMinutes", 8);
    }

    @Test
    void legacyIncidentEvent_isAcceptedAndMappedToIncidentContract() {
        var response = ingestionService.ingest(List.of(Map.of(
                "schemaVersion", 1,
                "reference", "INC-LEGACY-01",
                "type", "RETARD",
                "gravite", "ELEVE",
                "ligneTransport", "L2",
                "latitude", 33.5731,
                "longitude", -7.5898,
                "dateIncident", "2026-06-02T10:00:00",
                "dateResolution", "2026-06-02T10:20:00"
        )), SourceType.INCIDENT);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        IncomingEvent saved = savedEvent();
        assertThat(saved.getSourceType()).isEqualTo(SourceType.INCIDENT);
        assertThat(saved.getSourceId()).isEqualTo("INC-LEGACY-01");
        assertThat(saved.getEventType()).isEqualTo("INCIDENT_DELAY");
        assertThat(saved.getLineId()).isEqualTo("L2");
        assertThat(saved.getZoneId()).isEqualTo("33.57,-7.59");
        assertThat(saved.getPayload())
                .containsEntry("incidentId", "INC-LEGACY-01")
                .containsEntry("type", "delay")
                .containsEntry("severity", "HIGH")
                .containsEntry("resolutionMinutes", 20L);
    }

    @Test
    void missingSchemaVersionStillRejectedForDirectIngestion() {
        var response = ingestionService.ingest(List.of(Map.of(
                "eventType", "TICKET_VALIDATED",
                "ticketId", "TCK-LEGACY-01",
                "userId", "USR-01",
                "redeemedAt", "2026-06-02T10:15:00Z"
        )), SourceType.TICKETING);

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectedReasons()).first().asString().contains("schemaVersion");
    }

    @SuppressWarnings("unchecked")
    private IncomingEvent savedEvent() {
        ArgumentCaptor<List<IncomingEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        return captor.getValue().get(0);
    }
}
