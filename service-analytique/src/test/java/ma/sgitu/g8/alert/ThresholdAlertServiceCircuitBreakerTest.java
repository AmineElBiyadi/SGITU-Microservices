package ma.sgitu.g8.alert;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.SnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies the Resilience4j circuit breaker on {@link AlertSender#send}.
 * Configured in test/resources/application.yml: window=5, threshold=50%, open-wait=5s.
 */
@SpringBootTest
class ThresholdAlertServiceCircuitBreakerTest {

    @Autowired
    private ThresholdAlertService thresholdAlertService;

    @Autowired
    private AlertSender alertSender;

    @MockBean
    private SnapshotRepository snapshotRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetMocks() {
        reset(restTemplate, snapshotRepository);
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> cb.reset());
    }

    // -------------------------------------------------------------------------
    // A – successful alert reaches G5
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("A – G5 is available → alert sent, no exception propagates")
    void g5Available_alertSent_noException() {
        StatSnapshot snapshot = StatSnapshot.builder()
                .schemaVersion(1).statId("INC_TOTAL").value(15.0).build();
        when(snapshotRepository.findFirstByStatIdOrderByComputedAtDesc(anyString()))
                .thenReturn(Optional.of(snapshot));
        when(snapshotRepository.findTop30ByStatIdOrderByComputedAtDesc(anyString()))
                .thenReturn(java.util.List.of(snapshot));
        // Mock successful G5 call
        when(restTemplate.postForObject(anyString(), any(), eq(Void.class)))
                .thenReturn(null);

        assertThatCode(() -> thresholdAlertService.detect())
                .doesNotThrowAnyException();

        verify(restTemplate, atLeastOnce()).postForObject(anyString(), any(), eq(Void.class));
    }

    // -------------------------------------------------------------------------
    // B – G5 throws → fallback fires, no exception propagates
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("B – G5 throws ResourceAccessException → fallback fires, no exception propagates")
    void g5Unavailable_fallbackFires_noException() {
        StatSnapshot snapshot = StatSnapshot.builder()
                .schemaVersion(1).statId("INC_TOTAL").value(15.0).build();
        when(snapshotRepository.findFirstByStatIdOrderByComputedAtDesc("INC_TOTAL"))
                .thenReturn(Optional.of(snapshot));
        when(snapshotRepository.findFirstByStatIdOrderByComputedAtDesc(anyString()))
                .thenReturn(Optional.of(snapshot));
        when(snapshotRepository.findTop30ByStatIdOrderByComputedAtDesc(anyString()))
                .thenReturn(java.util.List.of(snapshot));

        doThrow(new ResourceAccessException("G5 connection refused"))
                .when(restTemplate).postForObject(anyString(), any(), eq(Void.class));

        assertThatCode(() -> thresholdAlertService.detect())
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // C – circuit opens after repeated failures, then fallback is called directly
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("C – circuit opens after threshold failures → subsequent calls use fallback without hitting G5")
    void circuitOpens_afterRepeatedFailures_g5NotCalledWhenOpen() {
        StatSnapshot snapshot = StatSnapshot.builder()
                .schemaVersion(1).statId("INC_TOTAL").value(15.0).build();
        when(snapshotRepository.findFirstByStatIdOrderByComputedAtDesc(anyString()))
                .thenReturn(Optional.of(snapshot));
        when(snapshotRepository.findTop30ByStatIdOrderByComputedAtDesc(anyString()))
                .thenReturn(java.util.List.of(snapshot));

        doThrow(new ResourceAccessException("G5 down"))
                .when(restTemplate).postForObject(anyString(), any(), eq(Void.class));

        // Trip the circuit breaker via AlertSender directly (5 calls, all fail).
        // Window size=5, threshold=50% → 5/5 failures = 100%, circuit opens.
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> alertSender.send(java.util.Map.of("eventType", "TEST")))
                    .doesNotThrowAnyException();
        }

        // Circuit is now OPEN. Reset the mock to track NEW calls only.
        reset(restTemplate);

        // Next send() must NOT hit restTemplate — fallback intercepts.
        assertThatCode(() -> alertSender.send(java.util.Map.of("eventType", "TEST")))
                .doesNotThrowAnyException();

        verifyNoInteractions(restTemplate);
    }

    // -------------------------------------------------------------------------
    // D – no snapshot → no alert, G5 never called
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("D – no snapshot in DB → detect() runs cleanly, G5 never called")
    void noSnapshot_g5NeverCalled() {
        when(snapshotRepository.findFirstByStatIdOrderByComputedAtDesc(anyString()))
                .thenReturn(Optional.empty());
        when(snapshotRepository.findTop30ByStatIdOrderByComputedAtDesc(anyString()))
                .thenReturn(java.util.List.of());

        assertThatCode(() -> thresholdAlertService.detect())
                .doesNotThrowAnyException();

        verifyNoInteractions(restTemplate);
    }
}
