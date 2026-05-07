package ma.sgitu.g8.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "incoming_events")
public class IncomingEvent {

    @Id
    private String id;
    private SourceType sourceType;
    private String sourceId;
    private String eventType;
    private Map<String, Object> payload;
    private LocalDateTime timestamp;
    private LocalDateTime receivedAt;
    private String lineId;
    private String zoneId;
    private boolean processed;
}
