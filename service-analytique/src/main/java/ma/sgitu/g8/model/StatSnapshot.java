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
@Document(collection = "stat_snapshots")
public class StatSnapshot {

    @Id
    private String id;
    private SnapshotType snapshotType;
    private String statId;
    private String period;
    private String granularity;
    private Double value;
    private Map<String, Object> metadata;
    private LocalDateTime computedAt;
    private boolean isPrediction;
}
