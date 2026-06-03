package ma.sgitu.g8.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.SnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsMetricsServiceTest {

    @Mock
    private SnapshotRepository snapshotRepository;

    @Mock
    private EventRepository eventRepository;

    private SimpleMeterRegistry meterRegistry;
    private AnalyticsMetricsService analyticsMetricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        analyticsMetricsService = new AnalyticsMetricsService(meterRegistry, snapshotRepository, eventRepository);
    }

    @Test
    void refreshMetrics_registersSnapshotAndZoneValues() {
        when(snapshotRepository.findFirstByStatIdOrderByComputedAtDesc(anyString()))
                .thenAnswer(invocation -> {
                    String statId = invocation.getArgument(0);
                    if ("FREQ_TOTAL_VALIDATIONS".equals(statId)) {
                        return Optional.of(snapshot(150.0));
                    }
                    if ("INC_BY_ZONE".equals(statId)) {
                        return Optional.of(zoneSnapshot());
                    }
                    return Optional.empty();
                });
        when(eventRepository.countBySourceType(SourceType.TICKETING)).thenReturn(42L);
        when(eventRepository.countBySourceType(SourceType.USER)).thenReturn(0L);
        when(eventRepository.countBySourceType(SourceType.SUBSCRIPTION)).thenReturn(0L);
        when(eventRepository.countBySourceType(SourceType.PAYMENT)).thenReturn(0L);
        when(eventRepository.countBySourceType(SourceType.VEHICLE)).thenReturn(0L);
        when(eventRepository.countBySourceType(SourceType.INCIDENT)).thenReturn(0L);

        analyticsMetricsService.refreshMetrics();

        assertThat(meterRegistry.find("sgitu_freq_total_validations").gauge().value()).isEqualTo(150.0);
        assertThat(meterRegistry.find("sgitu_inc_by_zone").tags("zone", "33.57,-7.59").gauge().value()).isEqualTo(3.0);
        assertThat(meterRegistry.find("sgitu_events_ingested_total").tags("source", "G2").gauge().value()).isEqualTo(42.0);
    }

    private StatSnapshot snapshot(double value) {
        return StatSnapshot.builder()
                .snapshotType(SnapshotType.TRIPS)
                .statId("FREQ_TOTAL_VALIDATIONS")
                .value(value)
                .build();
    }

    private StatSnapshot zoneSnapshot() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("byZone", List.of(Map.of("zone", "33.57,-7.59", "count", 3L)));
        data.put("total", 3L);
        return StatSnapshot.builder()
                .snapshotType(SnapshotType.INCIDENTS)
                .statId("INC_BY_ZONE")
                .metadata(Map.of("data", data))
                .build();
    }
}
