package ma.sgitu.g8.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Request to launch a data analysis")
public class AnalysisRequest {

    @NotBlank(message = "Dataset ID is required")
    @Schema(description = "Identifier of the dataset to analyze", example = "dataset-123")
    private String datasetId;

    @NotNull(message = "Analysis type is required")
    @Schema(description = "Type of analysis to perform", 
            allowableValues = {"statistique", "regression", "classification"},
            example = "statistique")
    private AnalysisType type;

    @Schema(description = "Analysis parameters specific to the type", 
            example = "{\"algorithm\": \"linear_regression\", \"features\": [\"age\", \"income\"]}")
    private Map<String, Object> parameters;

    @Schema(description = "Analysis type enumeration")
    public enum AnalysisType {
        STATISTIQUE("statistique"),
        REGRESSION("regression"),
        CLASSIFICATION("classification");

        private final String value;

        AnalysisType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
