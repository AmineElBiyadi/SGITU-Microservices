package ma.sgitu.g8.dto;

import lombok.Data;
import ma.sgitu.g8.model.SnapshotType;
import java.util.List;

@Data
public class ReportRequest {
    private String period;
    private List<SnapshotType> types;
}
