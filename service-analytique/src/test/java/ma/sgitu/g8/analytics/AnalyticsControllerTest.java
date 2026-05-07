package ma.sgitu.g8.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.ReportRepository;
import ma.sgitu.g8.repository.StatSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ma.sgitu.g8.controller.AnalyticsController}.
 *
 * The tests use the real Spring context + real MongoDB.
 * Repositories are cleared before each test.  Where a specific report id is
 * required (scenarios I/J) the id returned by scenario H (POST /reports/generate)
 * is captured and reused via a shared instance field within the same test class
 * execution.  Because JUnit runs tests in declaration order by default inside a
 * single class, and scenario H runs before I and J in their own dedicated tests,
 * the field is populated in time.  If test ordering is non-deterministic in a
 * particular Maven version the field falls back gracefully (see scenario J).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StatSnapshotRepository statSnapshotRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private EventRepository eventRepository;

    /** Shared across test methods so scenario I can use the id from scenario H. */
    private static String generatedReportId;
    private static String reportPeriod;

    @BeforeEach
    void clearReports() {
        reportRepository.deleteAll();
        statSnapshotRepository.deleteAll();
        eventRepository.deleteAll();
        generatedReportId = null;
        reportPeriod = null;
    }

    // -------------------------------------------------------------------------
    // A – GET /api/v1/analytics/dashboard
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("A – GET /api/v1/analytics/dashboard → 200, JSON array")
    void dashboardReturns200AndArray() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // -------------------------------------------------------------------------
    // B – GET /api/v1/analytics/trips/summary
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("B – GET /api/v1/analytics/trips/summary → 200")
    void tripsSummaryReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trips/summary"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // C – GET /api/v1/analytics/revenue/summary
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("C – GET /api/v1/analytics/revenue/summary → 200")
    void revenueSummaryReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/revenue/summary"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // D – GET /api/v1/analytics/incidents/stats
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("D – GET /api/v1/analytics/incidents/stats → 200")
    void incidentsStatsReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/incidents/stats"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // E – GET /api/v1/analytics/vehicles/activity
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("E – GET /api/v1/analytics/vehicles/activity → 200")
    void vehiclesActivityReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/vehicles/activity"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // F – GET /api/v1/analytics/users/stats
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("F – GET /api/v1/analytics/users/stats → 200")
    void usersStatsReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/users/stats"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // G – GET /api/v1/analytics/subscriptions/stats
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("G – GET /api/v1/analytics/subscriptions/stats → 200")
    void subscriptionsStatsReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/subscriptions/stats"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // H – POST /api/v1/analytics/reports/generate → 200, has id
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("H – POST /api/v1/analytics/reports/generate → 200 with report id")
    void generateReportReturnsIdAndPeriod() throws Exception {
        reportPeriod = "2026-05-03";
        Map<String, Object> requestBody = Map.of(
                "period", reportPeriod,
                "types", List.of("TRIPS", "REVENUE", "INCIDENTS")
        );
        String body = objectMapper.writeValueAsString(requestBody);

        MvcResult result = mockMvc.perform(post("/api/v1/analytics/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.period").value(reportPeriod))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Map<?, ?> response = objectMapper.readValue(responseJson, Map.class);
        generatedReportId = (String) response.get("id");
    }

    // -------------------------------------------------------------------------
    // I – GET /api/v1/analytics/reports/{id} with valid id → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("I – GET /api/v1/analytics/reports/{id} (valid) → 200 with matching period")
    void getReportByIdReturnsReport() throws Exception {
        // Create a report first so we have a real id
        reportPeriod = "2026-05-03";
        Map<String, Object> requestBody = Map.of(
                "period", reportPeriod,
                "types", List.of("TRIPS")
        );
        MvcResult createResult = mockMvc.perform(post("/api/v1/analytics/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), Map.class);
        String id = (String) created.get("id");

        mockMvc.perform(get("/api/v1/analytics/reports/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.period").value(reportPeriod));
    }

    // -------------------------------------------------------------------------
    // J – GET /api/v1/analytics/reports/{id} with invalid id → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("J – GET /api/v1/analytics/reports/{id} (invalid id) → 404")
    void getReportByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/reports/{id}", "nonexistent-id-12345"))
                .andExpect(status().isNotFound());
    }
}
