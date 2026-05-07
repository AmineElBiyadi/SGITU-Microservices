package ma.sgitu.g8.repository;

import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SourceType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends MongoRepository<IncomingEvent, String> {

    List<IncomingEvent> findBySourceTypeAndProcessedFalse(SourceType sourceType);

    List<IncomingEvent> findBySourceTypeAndTimestampBetween(
            SourceType sourceType,
            LocalDateTime from,
            LocalDateTime to
    );

    long countBySourceTypeAndProcessedFalse(SourceType sourceType);
}
