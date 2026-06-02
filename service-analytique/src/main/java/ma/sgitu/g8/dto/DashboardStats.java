package ma.sgitu.g8.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Global system statistics for dashboard")
public class DashboardStats {

    @Schema(description = "Total number of analyses in the system", example = "150")
    private Long totalAnalyses;

    @Schema(description = "Number of completed analyses", example = "120")
    private Long completedAnalyses;

    @Schema(description = "Number of failed analyses", example = "15")
    private Long failedAnalyses;

    @Schema(description = "Number of currently running analyses", example = "5")
    private Long runningAnalyses;

    @Schema(description = "Number of pending analyses", example = "10")
    private Long pendingAnalyses;

    @Schema(description = "Average execution time in milliseconds", example = "250000")
    private Double averageExecutionTimeMs;

    @Schema(description = "Success rate percentage", example = "88.89")
    private Double successRate;

    @Schema(description = "Number of analyses by type", example = "{\"statistique\": 80, \"regression\": 45, \"classification\": 25}")
    private AnalysisTypeStats analysesByType;

    @Data
    @Schema(description = "Statistics grouped by analysis type")
    public static class AnalysisTypeStats {
        
        @Schema(description = "Number of statistical analyses", example = "80")
        private Long statistique;

        @Schema(description = "Number of regression analyses", example = "45")
        private Long regression;

        @Schema(description = "Number of classification analyses", example = "25")
        private Long classification;
    }
}
