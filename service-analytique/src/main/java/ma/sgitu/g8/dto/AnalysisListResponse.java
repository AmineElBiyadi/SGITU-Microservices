package ma.sgitu.g8.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Paginated response containing list of analyses")
public class AnalysisListResponse {

    @Schema(description = "List of analyses")
    private List<AnalysisSummary> content;

    @Schema(description = "Total number of analyses", example = "150")
    private Long totalElements;

    @Schema(description = "Total number of pages", example = "8")
    private Integer totalPages;

    @Schema(description = "Pagination information")
    private Pageable pageable;

    @Data
    @Schema(description = "Summary information for each analysis")
    public static class AnalysisSummary {
        
        @Schema(description = "Unique identifier of the analysis", example = "analysis-456")
        private String analysisId;

        @Schema(description = "Current status of the analysis", example = "COMPLETED")
        private String status;

        @Schema(description = "Type of analysis performed", example = "statistique")
        private String type;

        @Schema(description = "Dataset identifier used for analysis", example = "dataset-123")
        private String datasetId;

        @Schema(description = "Timestamp when the analysis was created", example = "2024-01-15T10:30:00")
        private String createdAt;
    }

    @Data
    @Schema(description = "Pagination metadata")
    public static class Pageable {
        
        @Schema(description = "Current page number (0-based)", example = "0")
        private Integer pageNumber;

        @Schema(description = "Page size", example = "20")
        private Integer pageSize;

        @Schema(description = "Sort information", example = "createdAt,desc")
        private String sort;
    }
}
