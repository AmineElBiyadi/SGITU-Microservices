package ma.sgitu.g8.ml;

import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.service.SnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MlPredictionServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SnapshotService snapshotService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MlPredictionService mlPredictionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mlPredictionService, "mlServiceUrl", "http://ml-service:5000");
    }

    @Test
    @DisplayName("computePeakHoursPrediction skips cleanly when there is no ticket data")
    void peakHoursPredictionEmptyDatabase() {
        when(eventRepository.findBySourceTypeAndTimestampBetween(eq(SourceType.TICKETING), any(), any()))
                .thenReturn(List.of());

        assertThatCode(() -> mlPredictionService.computePeakHoursPrediction())
                .doesNotThrowAnyException();

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("computeIncidentPrediction skips cleanly when there is no incident data")
    void incidentPredictionEmptyDatabase() {
        when(eventRepository.findBySourceTypeAndTimestampBetween(eq(SourceType.INCIDENT), any(), any()))
                .thenReturn(List.of());

        assertThatCode(() -> mlPredictionService.computeIncidentPrediction())
                .doesNotThrowAnyException();

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("ML service outages do not propagate exceptions to callers")
    void peakHoursPredictionHandlesMlServiceFailure() {
        when(eventRepository.findBySourceTypeAndTimestampBetween(eq(SourceType.TICKETING), any(), any()))
                .thenReturn(List.of(buildTicketEvent("user-1", 8), buildTicketEvent("user-2", 8)));
        when(restTemplate.postForObject(contains("/predict/peak-hours"), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        assertThatCode(() -> mlPredictionService.computePeakHoursPrediction())
                .doesNotThrowAnyException();
    }

    private IncomingEvent buildTicketEvent(String userId, int hour) {
        return IncomingEvent.builder()
                .sourceType(SourceType.TICKETING)
                .sourceId(userId)
                .eventType("TICKET_VALIDATED")
                .timestamp(LocalDateTime.now().minusDays(1).withHour(hour))
                .payload(Map.of("status", "validated", "userId", userId))
                .processed(false)
                .build();
    }
}
