package ma.sgitu.g8.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "Response containing analysis creation information")
public class AnalysisResponse {

    @Schema(description = "Unique identifier of the analysis", example = "analysis-456")
    private String analysisId;

    @Schema(description = "Current status of the analysis", 
            allowableValues = {"PENDING", "RUNNING", "COMPLETED", "FAILED"},
            example = "PENDING")
    private String status;

    @Schema(description = "Timestamp when the analysis was created", 
            example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Type of analysis performed", example = "statistique")
    private String type;

    @Schema(description = "Dataset identifier used for analysis", example = "dataset-123")
    private String datasetId;
}
