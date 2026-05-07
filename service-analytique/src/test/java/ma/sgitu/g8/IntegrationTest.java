package ma.sgitu.g8;

import ma.sgitu.g8.model.Report;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.ReportRepository;
import ma.sgitu.g8.repository.SnapshotRepository;
import ma.sgitu.g8.scheduler.ScheduledAnalyticsJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ScheduledAnalyticsJob scheduledAnalyticsJob;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    void setup() {
        eventRepository.deleteAll();
        snapshotRepository.deleteAll();
        reportRepository.deleteAll();
    }

    private String createTimestamp(int hour) {
        return OffsetDateTime.now(ZoneOffset.UTC)
                .minusDays(1)
                .withHour(hour)
                .withMinute(0)
                .withSecond(0)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Test
    @DisplayName("End-to-End Pipeline: Ingestion -> Processing -> Analytics -> Reporting")
    void fullPipelineTest() {
        // =====================================================================
        // Step 1 - Ingest mock events via REST
        // =====================================================================

        // 10 ticket events
        List<Map<String, Object>> tickets = new ArrayList<>();
        int[] hours = {6, 8, 9, 12, 17, 18, 6, 8, 17, 18};
        String[] lines = {"L1", "L2", "L3", "L1", "L2", "L3", "L1", "L2", "L3", "L1"};
        for (int i = 0; i < 10; i++) {
            tickets.add(Map.of(
                    "timestamp", createTimestamp(hours[i]),
                    "userId", UUID.randomUUID().toString(),
                    "status", "validated",
                    "line", lines[i],
                    "stationId", "ST-" + (i % 3)
            ));
        }
        ResponseEntity<Map> ticketResp = restTemplate.postForEntity(
                "/api/v1/ingestion/tickets", tickets, Map.class);
        assertThat(ticketResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 5 payment events
        List<Map<String, Object>> payments = new ArrayList<>();
        double[] amounts = {10.0, 20.0, 5.0, 50.0, 15.0};
        String[] methods = {"CARD", "CASH", "CARD", "MOBILE", "CASH"};
        for (int i = 0; i < 5; i++) {
            payments.add(Map.of(
                    "timestamp", createTimestamp(10),
                    "transactionId", UUID.randomUUID().toString(),
                    "status", "completed",
                    "amount", amounts[i],
                    "paymentMethod", methods[i],
                    "paymentType", "TICKET"
            ));
        }
        ResponseEntity<Map> paymentResp = restTemplate.postForEntity(
                "/api/v1/ingestion/payments", payments, Map.class);
        assertThat(paymentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 5 incident events
        List<Map<String, Object>> incidents = new ArrayList<>();
        String[] zones = {"Z1", "Z2", "Z3", "Z1", "Z2"};
        String[] severities = {"LOW", "MEDIUM", "HIGH", "CRITICAL", "LOW"};
        for (int i = 0; i < 5; i++) {
            incidents.add(Map.of(
                    "timestamp", createTimestamp(10),
                    "incidentId", UUID.randomUUID().toString(),
                    "type", "delay",
                    "zone", zones[i],
                    "severity", severities[i]
            ));
        }
        ResponseEntity<Map> incidentResp = restTemplate.postForEntity(
                "/api/v1/ingestion/incidents", incidents, Map.class);
        assertThat(incidentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);


        // =====================================================================
        // Step 2 - Trigger the scheduler manually
        // =====================================================================
        scheduledAnalyticsJob.runAnalytics();


        // =====================================================================
        // Step 3 - Verify snapshots were created
        // =====================================================================
        List<StatSnapshot> snapshots = snapshotRepository.findAll();
        assertThat(snapshots.size()).isGreaterThanOrEqualTo(10);

        boolean hasFreq = snapshots.stream().anyMatch(s -> s.getStatId().startsWith("FREQ_"));
        boolean hasRev = snapshots.stream().anyMatch(s -> s.getStatId().startsWith("REV_"));
        boolean hasInc = snapshots.stream().anyMatch(s -> s.getStatId().startsWith("INC_"));

        assertThat(hasFreq).as("Should have FREQ_ snapshots").isTrue();
        assertThat(hasRev).as("Should have REV_ snapshots").isTrue();
        assertThat(hasInc).as("Should have INC_ snapshots").isTrue();


        // =====================================================================
        // Step 4 - Verify analytics endpoints return data
        // =====================================================================
        ResponseEntity<List> dashboardResp = restTemplate.getForEntity(
                "/api/v1/analytics/dashboard", List.class);
        assertThat(dashboardResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboardResp.getBody()).isNotEmpty();


        // =====================================================================
        // Step 5 - Verify report generation works
        // =====================================================================
        String reportPeriod = LocalDateTime.now().toLocalDate().toString();
        Map<String, Object> reportReq = Map.of(
                "period", reportPeriod,
                "types", List.of("TRIPS", "REVENUE", "INCIDENTS")
        );

        ResponseEntity<Report> generateResp = restTemplate.postForEntity(
                "/api/v1/analytics/reports/generate", reportReq, Report.class);
        assertThat(generateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Report generatedReport = generateResp.getBody();
        assertThat(generatedReport).isNotNull();
        assertThat(generatedReport.getId()).isNotNull();

        // Get the generated report
        ResponseEntity<Report> getReportResp = restTemplate.getForEntity(
                "/api/v1/analytics/reports/" + generatedReport.getId(), Report.class);
        assertThat(getReportResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getReportResp.getBody()).isNotNull();
        assertThat(getReportResp.getBody().getId()).isEqualTo(generatedReport.getId());
        assertThat(getReportResp.getBody().getPeriod()).isEqualTo(reportPeriod);
    }
}
