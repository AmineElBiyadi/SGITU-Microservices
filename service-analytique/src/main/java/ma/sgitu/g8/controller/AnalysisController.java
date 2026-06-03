package ma.sgitu.g8.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.dto.*;
import ma.sgitu.g8.service.AnalysisService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/analyses", "/api/analytics"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Analysis API", description = "Endpoints for launching, monitoring and managing data analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    @Operation(summary = "Launch a new data analysis", 
              description = "Starts a new analysis on the specified dataset with the given parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Analysis successfully launched",
                    content = @Content(schema = @Schema(implementation = AnalysisResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Dataset not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AnalysisResponse> launchAnalysis(
            @Valid @RequestBody AnalysisRequest request) {
        
        log.info("Launching {} analysis on dataset: {}", request.getType(), request.getDatasetId());
        
        try {
            AnalysisResponse response = analysisService.launchAnalysis(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid analysis request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error launching analysis: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{analysisId}")
    @Operation(summary = "Get analysis status and results", 
              description = "Retrieves the current status and results of a specific analysis")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analysis details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AnalysisDetailResponse.class))),
        @ApiResponse(responseCode = "404", description = "Analysis not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AnalysisDetailResponse> getAnalysis(
            @Parameter(description = "Unique identifier of the analysis", required = true, 
                      example = "analysis-456")
            @PathVariable String analysisId) {
        
        log.info("Retrieving analysis details for ID: {}", analysisId);
        
        return analysisService.getAnalysisById(analysisId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all analyses", 
              description = "Retrieves a paginated list of all analyses with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analyses list retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AnalysisListResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AnalysisListResponse> listAnalyses(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Filter by analysis status", 
                      example = "COMPLETED", schema = @Schema(allowableValues = {"PENDING", "RUNNING", "COMPLETED", "FAILED"}))
            @RequestParam(required = false) String status) {
        
        log.info("Listing analyses - page: {}, size: {}, status: {}", page, size, status);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<AnalysisListResponse.AnalysisSummary> analysesPage = 
                analysisService.listAnalyses(pageable, status);
            
            AnalysisListResponse response = new AnalysisListResponse();
            response.setContent(analysesPage.getContent());
            response.setTotalElements(analysesPage.getTotalElements());
            response.setTotalPages(analysesPage.getTotalPages());
            
            AnalysisListResponse.Pageable pageableInfo = new AnalysisListResponse.Pageable();
            pageableInfo.setPageNumber(analysesPage.getNumber());
            pageableInfo.setPageSize(analysesPage.getSize());
            pageableInfo.setSort(analysesPage.getSort().toString());
            response.setPageable(pageableInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error listing analyses: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{analysisId}")
    @Operation(summary = "Delete an analysis", 
              description = "Removes an analysis and all its associated results from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Analysis successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Analysis not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Cannot delete running analysis",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteAnalysis(
            @Parameter(description = "Unique identifier of the analysis", required = true, 
                      example = "analysis-456")
            @PathVariable String analysisId) {
        
        log.info("Deleting analysis with ID: {}", analysisId);
        
        try {
            boolean deleted = analysisService.deleteAnalysis(analysisId);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Cannot delete running analysis: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error deleting analysis: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get system dashboard statistics", 
              description = "Retrieves global statistics about all analyses in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DashboardStats.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DashboardStats> getDashboardStats() {
        
        log.info("Retrieving dashboard statistics");
        
        try {
            DashboardStats stats = analysisService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving dashboard statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

@Schema(description = "Error response")
class ErrorResponse {
    @Schema(description = "Error message", example = "Dataset not found")
    private String message;
    
    @Schema(description = "Error timestamp", example = "2024-01-15T10:30:00")
    private String timestamp;
    
    @Schema(description = "HTTP status code", example = "404")
    private int status;
}
