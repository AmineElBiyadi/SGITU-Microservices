package ma.sgitu.g8.scheduler;

import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.SnapshotRepository;
import ma.sgitu.g8.repository.StatSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for {@link ScheduledAnalyticsJob}.
 *
 * The scheduler is called directly (bypassing the 60-second timer) and the
 * results are asserted against the real MongoDB (Docker, localhost:27017).
 *
 * All aggregation classes catch their own exceptions, so runAnalytics() must
 * complete without propagating any exception even when the database is empty.
 */
@SpringBootTest
class ScheduledAnalyticsJobTest {

    @Autowired
    private ScheduledAnalyticsJob scheduledAnalyticsJob;

    @Autowired
    private EventRepository eventRepository;

    /** SnapshotRepository (used by aggregations to save). */
    @Autowired
    private SnapshotRepository snapshotRepository;

    /** StatSnapshotRepository (maps to the same collection, used for reads). */
    @Autowired
    private StatSnapshotRepository statSnapshotRepository;

    @BeforeEach
    void cleanDb() {
        eventRepository.deleteAll();
        statSnapshotRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Scenario A – job runs without errors on empty database
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("A – runAnalytics() completes without exception on empty DB")
    void runAnalyticsOnEmptyDatabase_noException() {
        assertThatCode(() -> scheduledAnalyticsJob.runAnalytics())
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Scenario B – job writes snapshots after ingesting mock events
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("B – runAnalytics() writes at least one snapshot after seeding events")
    void runAnalyticsAfterSeeding_createsSnapshots() {
        // ---- seed 5 IncomingEvent documents covering several source types ----
        List<IncomingEvent> events = new ArrayList<>();

        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(10);

        // TICKETING – validated ticket
        events.add(IncomingEvent.builder()
                .sourceType(SourceType.TICKETING)
                .sourceId("user-1")
                .eventType("TICKET_VALIDATED")
                .lineId("L1")
                .timestamp(baseTime)
                .receivedAt(LocalDateTime.now())
                .payload(Map.of("status", "validated", "line", "L1", "stationId", "ST-01"))
                .processed(false)
                .build());

        // PAYMENT – completed payment
        events.add(IncomingEvent.builder()
                .sourceType(SourceType.PAYMENT)
                .sourceId("tx-001")
                .eventType("PAYMENT_COMPLETED")
                .timestamp(baseTime)
                .receivedAt(LocalDateTime.now())
                .payload(Map.of("status", "SUCCESS", "amount", 25.50, "paymentMethod", "CARD", "paymentType", "TICKET"))
                .processed(false)
                .build());

        // INCIDENT
        events.add(IncomingEvent.builder()
                .sourceType(SourceType.INCIDENT)
                .sourceId("inc-001")
                .eventType("INCIDENT_DELAY")
                .zoneId("Z1")
                .timestamp(baseTime)
                .receivedAt(LocalDateTime.now())
                .payload(Map.of("type", "delay", "severity", "LOW", "zone", "Z1"))
                .processed(false)
                .build());

        // VEHICLE
        events.add(IncomingEvent.builder()
                .sourceType(SourceType.VEHICLE)
                .sourceId("v-001")
                .eventType("VEHICLE_IN_SERVICE")
                .lineId("L1")
                .timestamp(baseTime)
                .receivedAt(LocalDateTime.now())
                .payload(Map.of("status", "ACTIVE", "line", "L1", "avgSpeed", 55))
                .processed(false)
                .build());

        // SUBSCRIPTION
        events.add(IncomingEvent.builder()
                .sourceType(SourceType.SUBSCRIPTION)
                .sourceId("sub-user-1")
                .eventType("SUBSCRIPTION_CREATED")
                .timestamp(baseTime)
                .receivedAt(LocalDateTime.now())
                .payload(Map.of("action", "NEW", "subscriptionType", "monthly"))
                .processed(false)
                .build());

        eventRepository.saveAll(events);

        // ---- run the analytics job ----
        scheduledAnalyticsJob.runAnalytics();

        // ---- assert at least one snapshot was written ----
        List<StatSnapshot> snapshots = statSnapshotRepository.findAll();
        assertThat(snapshots).isNotEmpty();
    }

    // -------------------------------------------------------------------------
    // Scenario C – snapshot has correct structure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("C – snapshots have non-null statId, snapshotType, and computedAt")
    void snapshots_haveRequiredFields() {
        // Seed at least one event so the job produces at least one snapshot
        LocalDateTime now = LocalDateTime.now().minusMinutes(5);
        eventRepository.save(IncomingEvent.builder()
                .sourceType(SourceType.TICKETING)
                .sourceId("user-struct")
                .eventType("TICKET_VALIDATED")
                .lineId("L1")
                .timestamp(now)
                .receivedAt(LocalDateTime.now())
                .payload(Map.of("status", "validated", "line", "L1"))
                .processed(false)
                .build());

        scheduledAnalyticsJob.runAnalytics();

        List<StatSnapshot> snapshots = statSnapshotRepository.findAll();
        assertThat(snapshots).isNotEmpty();

        for (StatSnapshot snapshot : snapshots) {
            assertThat(snapshot.getStatId())
                    .as("statId must not be null for snapshot %s", snapshot.getId())
                    .isNotNull();
            assertThat(snapshot.getSnapshotType())
                    .as("snapshotType must not be null for snapshot %s", snapshot.getId())
                    .isNotNull();
            assertThat(snapshot.getComputedAt())
                    .as("computedAt must not be null for snapshot %s", snapshot.getId())
                    .isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // Scenario D – Upsert creates exactly one snapshot per statId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("D – Call runAnalytics() twice, expect exactly one snapshot per statId")
    void runAnalyticsTwice_validatesUpsert() {
        LocalDateTime now = LocalDateTime.now().minusMinutes(5);
        eventRepository.save(IncomingEvent.builder()
                .sourceType(SourceType.TICKETING)
                .sourceId("user-upsert")
                .eventType("TICKET_VALIDATED")
                .lineId("L1")
                .timestamp(now)
                .receivedAt(LocalDateTime.now())
                .payload(Map.of("status", "validated", "line", "L1"))
                .processed(false)
                .build());

        scheduledAnalyticsJob.runAnalytics();
        scheduledAnalyticsJob.runAnalytics();

        List<StatSnapshot> snapshots = statSnapshotRepository.findAll();
        assertThat(snapshots).isNotEmpty();

        Map<String, List<StatSnapshot>> groupedByStatId = new HashMap<>();
        for (StatSnapshot snapshot : snapshots) {
            groupedByStatId.computeIfAbsent(snapshot.getStatId(), k -> new ArrayList<>()).add(snapshot);
        }

        groupedByStatId.forEach((statId, docs) -> {
            assertThat(docs).as("statId %s should have exactly 1 document", statId).hasSize(1);
        });
    }
}
