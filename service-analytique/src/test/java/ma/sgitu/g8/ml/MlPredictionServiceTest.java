package ma.sgitu.g8.ml;

import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.StatSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link MlPredictionService}.
 *
 * Scenarios A and B verify graceful handling of an empty database (no data →
 * early return, no exception).
 *
 * Scenario C uses {@link MockBean} to replace the real {@link RestTemplate}
 * with a mock that throws {@link ResourceAccessException}, simulating an
 * unavailable Python ML service.  Because {@link MlPredictionService} wraps
 * every call in a broad try/catch, the exception must never propagate to the
 * caller.
 */
@SpringBootTest
class MlPredictionServiceTest {

    @Autowired
    private MlPredictionService mlPredictionService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private StatSnapshotRepository statSnapshotRepository;

    /**
     * Declared as @MockBean so Spring replaces the real RestTemplate bean in the
     * entire context for this test class.  This affects ThresholdAlertService too,
     * but that is acceptable because all alert sends are also wrapped in try/catch.
     */
    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void cleanDb() {
        eventRepository.deleteAll();
        statSnapshotRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Scenario A – computePeakHoursPrediction with no data
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("A – computePeakHoursPrediction() on empty DB → no exception, graceful skip")
    void peakHoursPrediction_emptyDatabase_noException() {
        // No events in DB – service must log a warning and return early
        assertThatCode(() -> mlPredictionService.computePeakHoursPrediction())
                .doesNotThrowAnyException();

        // RestTemplate must NOT have been called since there is no data
        verifyNoInteractions(restTemplate);
    }

    // -------------------------------------------------------------------------
    // Scenario B – computeIncidentPrediction with no data
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("B – computeIncidentPrediction() on empty DB → no exception, graceful skip")
    void incidentPrediction_emptyDatabase_noException() {
        assertThatCode(() -> mlPredictionService.computeIncidentPrediction())
                .doesNotThrowAnyException();

        verifyNoInteractions(restTemplate);
    }

    // -------------------------------------------------------------------------
    // Scenario C – ML service unavailable
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("C – ML service unavailable (ResourceAccessException) → no exception propagates")
    void peakHoursPrediction_mlServiceDown_noException() {
        // Insert validated ticket data so the service reaches the RestTemplate call
        LocalDateTime pastHour = LocalDateTime.now().minusHours(1);
        eventRepository.saveAll(List.of(
                buildTicketEvent("user-A", pastHour, "L1"),
                buildTicketEvent("user-B", pastHour.minusMinutes(15), "L1"),
                buildTicketEvent("user-C", pastHour.minusMinutes(30), "L2")
        ));

        // Mock RestTemplate to simulate a network failure when calling the ML service
        when(restTemplate.postForObject(
                contains("/predict/peak-hours"),
                any(),
                eq(Map.class)
        )).thenThrow(new ResourceAccessException("ML service connection refused"));

        // The service must catch the exception internally and not propagate it
        assertThatCode(() -> mlPredictionService.computePeakHoursPrediction())
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private IncomingEvent buildTicketEvent(String userId, LocalDateTime ts, String line) {
        return IncomingEvent.builder()
                .sourceType(SourceType.TICKETING)
                .sourceId(userId)
                .eventType("TICKET_VALIDATED")
                .lineId(line)
                .timestamp(ts)
                .receivedAt(LocalDateTime.now())
                .payload(Map.of(
                        "status", "validated",
                        "line", line,
                        "userId", userId
                ))
                .processed(false)
                .build();
    }
}
