package ma.sgitu.g8.etl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.StatSnapshotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlPipelineService {

    private final EventRepository eventRepository;
    private final StatSnapshotRepository statSnapshotRepository;

    /**
     * ETL Pipeline executed every hour.
     * Extract: Fetch events from the last hour.
     * Transform: Group by source type and calculate metrics.
     * Load: Save metrics into StatSnapshot collection.
     */
    @Scheduled(cron = "0 0 * * * *") // Runs at the top of every hour
    public void runEtlPipeline() {
        log.info("Starting ETL Pipeline...");
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(1);

        // 1. EXTRACT
        for (SourceType sourceType : SourceType.values()) {
            List<IncomingEvent> events = eventRepository.findBySourceTypeAndTimestampBetween(sourceType, start, end);
            log.info("Extracted {} events of type {} from {} to {}", events.size(), sourceType, start, end);

            if (events.isEmpty()) {
                continue;
            }

            // 2. TRANSFORM
            Map<String, List<IncomingEvent>> groupedEvents = events.stream()
                    .filter(e -> e.getSourceId() != null)
                    .collect(Collectors.groupingBy(IncomingEvent::getSourceId));

            for (Map.Entry<String, List<IncomingEvent>> entry : groupedEvents.entrySet()) {
                String sourceId = entry.getKey();
                List<IncomingEvent> sourceEvents = entry.getValue();

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("eventCount", sourceEvents.size());
                metrics.put("processedCount", sourceEvents.stream().filter(IncomingEvent::isProcessed).count());

                // 3. LOAD
                StatSnapshot snapshot = new StatSnapshot();
                snapshot.setId(UUID.randomUUID().toString());
                snapshot.setStatId(sourceId);
                
                SnapshotType snapshotType = switch (sourceType) {
                    case VEHICLE -> SnapshotType.VEHICLES;
                    case INCIDENT -> SnapshotType.INCIDENTS;
                    case PAYMENT -> SnapshotType.REVENUE;
                    case USER -> SnapshotType.USERS;
                    case TICKETING -> SnapshotType.TRIPS;
                    case SUBSCRIPTION -> SnapshotType.SUBSCRIPTIONS;
                };
                
                snapshot.setSnapshotType(snapshotType);
                snapshot.setComputedAt(end);
                snapshot.setPeriod("HOURLY");
                snapshot.setMetadata(metrics);

                statSnapshotRepository.save(snapshot);
            }
        }

        log.info("ETL Pipeline finished successfully.");
    }
}
