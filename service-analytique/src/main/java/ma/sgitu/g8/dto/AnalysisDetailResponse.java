package ma.sgitu.g8.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "Detailed response containing analysis status and results")
public class AnalysisDetailResponse {

    @Schema(description = "Unique identifier of the analysis", example = "analysis-456")
    private String analysisId;

    @Schema(description = "Current status of the analysis", 
            allowableValues = {"PENDING", "RUNNING", "COMPLETED", "FAILED"},
            example = "COMPLETED")
    private String status;

    @Schema(description = "Analysis results (populated when status is COMPLETED)", 
            example = "{\"accuracy\": 0.95, \"precision\": 0.92, \"recall\": 0.88}")
    private Map<String, Object> result;

    @Schema(description = "Error message (populated when status is FAILED)", 
            example = "Insufficient data for analysis")
    private String errorMessage;

    @Schema(description = "Timestamp when the analysis started", 
            example = "2024-01-15T10:30:00")
    private LocalDateTime startedAt;

    @Schema(description = "Timestamp when the analysis completed", 
            example = "2024-01-15T10:35:00")
    private LocalDateTime completedAt;

    @Schema(description = "Type of analysis performed", example = "statistique")
    private String type;

    @Schema(description = "Dataset identifier used for analysis", example = "dataset-123")
    private String datasetId;

    @Schema(description = "Execution time in milliseconds", example = "300000")
    private Long executionTimeMs;
}
