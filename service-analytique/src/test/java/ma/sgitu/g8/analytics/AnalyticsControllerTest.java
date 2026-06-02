package ma.sgitu.g8.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.sgitu.g8.controller.AnalyticsController;
import ma.sgitu.g8.model.Report;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("GET /api/v1/analytics/dashboard returns snapshots")
    void dashboardReturnsSnapshots() throws Exception {
        StatSnapshot snapshot = StatSnapshot.builder()
                .id("snap-1")
                .snapshotType(SnapshotType.DASHBOARD)
                .statId("DASH_01")
                .period("2026-05-05")
                .computedAt(LocalDateTime.now())
                .metadata(Map.of("activeUsers", 42))
                .build();

        when(analyticsService.getAllSnapshots()).thenReturn(List.of(snapshot));

        mockMvc.perform(get("/api/v1/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statId").value("DASH_01"));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/trips/summary with a period returns a single snapshot")
    void tripsSummaryWithPeriodReturnsSnapshot() throws Exception {
        StatSnapshot snapshot = StatSnapshot.builder()
                .id("snap-trips")
                .snapshotType(SnapshotType.TRIPS)
                .statId("FREQ_01")
                .period("2026-05-05")
                .computedAt(LocalDateTime.now())
                .build();

        when(analyticsService.getSnapshotByTypeAndPeriod(SnapshotType.TRIPS, "2026-05-05"))
                .thenReturn(Optional.of(snapshot));

        mockMvc.perform(get("/api/v1/analytics/trips/summary").param("period", "2026-05-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statId").value("FREQ_01"));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/reports/{id} returns 404 when absent")
    void missingReportReturns404() throws Exception {
        when(analyticsService.getReportById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/analytics/reports/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/analytics/reports/generate returns the generated report")
    void generateReportReturnsReport() throws Exception {
        Report report = Report.builder()
                .id("report-1")
                .period("2026-05-05")
                .requestedTypes(List.of(SnapshotType.TRIPS, SnapshotType.REVENUE))
                .generatedAt(LocalDateTime.now())
                .snapshots(List.of())
                .build();

        when(analyticsService.generateReport(eq("2026-05-05"), eq(List.of(SnapshotType.TRIPS, SnapshotType.REVENUE))))
                .thenReturn(report);

        mockMvc.perform(post("/api/v1/analytics/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "period", "2026-05-05",
                                "types", List.of("TRIPS", "REVENUE")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("report-1"))
                .andExpect(jsonPath("$.period").value("2026-05-05"));
    }
}
