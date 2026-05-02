package ma.sgitu.g8.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.sgitu.g8.dto.ReportRequest;
import ma.sgitu.g8.model.Report;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Endpoints for retrieving analytics snapshots and generating reports")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    private ResponseEntity<?> getSnapshotResponse(SnapshotType type, String period) {
        if (period != null) {
            return analyticsService.getSnapshotByTypeAndPeriod(type, period)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        return ResponseEntity.ok(analyticsService.getSnapshotsByType(type));
    }

    @GetMapping("/trips/summary")
    @Operation(summary = "Get trips summary statistics")
    public ResponseEntity<?> getTripsSummary(@RequestParam(required = false) String period) {
        return getSnapshotResponse(SnapshotType.TRIPS, period);
    }

    @GetMapping("/revenue/summary")
    @Operation(summary = "Get revenue summary statistics")
    public ResponseEntity<?> getRevenueSummary(@RequestParam(required = false) String period) {
        return getSnapshotResponse(SnapshotType.REVENUE, period);
    }

    @GetMapping("/incidents/stats")
    @Operation(summary = "Get incidents statistics")
    public ResponseEntity<?> getIncidentsStats(@RequestParam(required = false) String period) {
        return getSnapshotResponse(SnapshotType.INCIDENTS, period);
    }

    @GetMapping("/vehicles/activity")
    @Operation(summary = "Get vehicles activity statistics")
    public ResponseEntity<?> getVehiclesActivity(@RequestParam(required = false) String period) {
        return getSnapshotResponse(SnapshotType.VEHICLES, period);
    }

    @GetMapping("/users/stats")
    @Operation(summary = "Get users statistics")
    public ResponseEntity<?> getUsersStats(@RequestParam(required = false) String period) {
        return getSnapshotResponse(SnapshotType.USERS, period);
    }

    @GetMapping("/subscriptions/stats")
    @Operation(summary = "Get subscriptions statistics")
    public ResponseEntity<?> getSubscriptionsStats(@RequestParam(required = false) String period) {
        return getSnapshotResponse(SnapshotType.SUBSCRIPTIONS, period);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard summary statistics")
    public ResponseEntity<?> getDashboard(@RequestParam(required = false) String period) {
        return getSnapshotResponse(SnapshotType.DASHBOARD, period);
    }

    @PostMapping("/reports/generate")
    @Operation(summary = "Generate a new report")
    public ResponseEntity<Report> generateReport(@RequestBody ReportRequest request) {
        Report report = analyticsService.generateReport(request.getPeriod(), request.getTypes());
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Retrieve a generated report by ID")
    public ResponseEntity<Report> getReportById(@PathVariable String id) {
        return analyticsService.getReportById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
