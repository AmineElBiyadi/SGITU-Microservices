package ma.sgitu.g8.ingestion.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchIngestionResponse {
    private int totalReceived;
    private int totalAccepted;
    private int totalRejected;
    private List<String> rejectedReasons;
    private String status;
}
