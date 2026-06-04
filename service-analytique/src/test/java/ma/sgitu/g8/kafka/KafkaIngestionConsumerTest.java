package ma.sgitu.g8.kafka;

import ma.sgitu.g8.ingestion.IngestionService;
import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import ma.sgitu.g8.model.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaIngestionConsumerTest {

    @Mock
    private IngestionService ingestionService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private KafkaIngestionConsumer consumer;

    private BatchIngestionResponse successResponse = BatchIngestionResponse.builder()
            .totalReceived(1).totalAccepted(1).totalRejected(0)
            .status("SUCCESS").build();

    private Map<String, Object> singleEvent() {
        return new LinkedHashMap<>(Map.of("id", "123", "schemaVersion", 1));
    }

    @Test
    @DisplayName("A1 – consumeTicketingEvents → ingest called with TICKETING")
    void ticketing_delegatesWithCorrectSourceType() {
        when(ingestionService.ingest(any(), eq(SourceType.TICKETING))).thenReturn(successResponse);
        consumer.consumeTicketingEvents(singleEvent(), ack);
        verify(ingestionService).ingest(any(), eq(SourceType.TICKETING));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("A2 – consumeSubscriptionEvents → ingest called with SUBSCRIPTION")
    void subscription_delegatesWithCorrectSourceType() {
        when(ingestionService.ingest(any(), eq(SourceType.SUBSCRIPTION))).thenReturn(successResponse);
        consumer.consumeSubscriptionEvents(singleEvent(), ack);
        verify(ingestionService).ingest(any(), eq(SourceType.SUBSCRIPTION));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("A3 – consumePaymentEvents → ingest called with PAYMENT")
    void payment_delegatesWithCorrectSourceType() {
        when(ingestionService.ingest(any(), eq(SourceType.PAYMENT))).thenReturn(successResponse);
        consumer.consumePaymentEvents(singleEvent(), ack);
        verify(ingestionService).ingest(any(), eq(SourceType.PAYMENT));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("A4 – consumeVehicleEvents → ingest called with VEHICLE")
    void vehicle_delegatesWithCorrectSourceType() {
        when(ingestionService.ingest(any(), eq(SourceType.VEHICLE))).thenReturn(successResponse);
        consumer.consumeVehicleEvents(singleEvent(), ack);
        verify(ingestionService).ingest(any(), eq(SourceType.VEHICLE));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("A5 – consumeIncidentEvents → ingest called with INCIDENT")
    void incident_delegatesWithCorrectSourceType() {
        when(ingestionService.ingest(any(), eq(SourceType.INCIDENT))).thenReturn(successResponse);
        consumer.consumeIncidentEvents(singleEvent(), ack);
        verify(ingestionService).ingest(any(), eq(SourceType.INCIDENT));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("A6 – consumeUserEvents → ingest called with USER")
    void user_delegatesWithCorrectSourceType() {
        when(ingestionService.ingest(any(), eq(SourceType.USER))).thenReturn(successResponse);
        consumer.consumeUserEvents(singleEvent(), ack);
        verify(ingestionService).ingest(any(), eq(SourceType.USER));
        verify(ack).acknowledge();
    }

    // -------------------------------------------------------------------------
    // B – IngestionService throws → no exception propagates, ack NOT called
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("B – IngestionService throws RuntimeException → no exception propagates, ack NOT called")
    void ingestionServiceThrows_noExceptionPropagates_ackNotCalled() {
        when(ingestionService.ingest(any(), eq(SourceType.TICKETING)))
                .thenThrow(new RuntimeException("DB down"));

        assertThatCode(() -> consumer.consumeTicketingEvents(singleEvent(), ack))
                .doesNotThrowAnyException();

        verify(ack, never()).acknowledge();
    }

    // -------------------------------------------------------------------------
    // C – PARTIAL result still acknowledges (not a failure)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("C – PARTIAL ingestion result → ack is still acknowledged")
    void partialResult_ackIsAcknowledged() {
        BatchIngestionResponse partial = BatchIngestionResponse.builder()
                .totalReceived(2).totalAccepted(1).totalRejected(1)
                .status("PARTIAL").rejectedReasons(List.of("Event 1: missing schemaVersion"))
                .build();
        when(ingestionService.ingest(any(), eq(SourceType.TICKETING))).thenReturn(partial);

        consumer.consumeTicketingEvents(singleEvent(), ack);

        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("D - missing schemaVersion is added before Kafka batch ingestion")
    void missingSchemaVersion_isAddedBeforeIngestion() {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", "2024-01-15T10:00:00Z");
        event.put("vehicleId", "veh-01");
        event.put("status", "in_service");
        event.put("line", "L1");

        when(ingestionService.ingest(any(), eq(SourceType.VEHICLE))).thenReturn(successResponse);

        consumer.consumeVehicleEvents(event, ack);

        ArgumentCaptor<List<Map<String, Object>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(ingestionService).ingest(eventsCaptor.capture(), eq(SourceType.VEHICLE));
        assertThat(eventsCaptor.getValue().get(0).get("schemaVersion")).isEqualTo(1);
        verify(ack).acknowledge();
    }
}
