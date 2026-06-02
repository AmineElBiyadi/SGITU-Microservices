package ma.sgitu.g8.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import ma.sgitu.g8.model.SourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngestionController.class)
@AutoConfigureMockMvc(addFilters = false)
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IngestionService ingestionService;

    @org.junit.jupiter.api.BeforeEach
    void setupGlobalMock() {
        when(ingestionService.ingest(anyList(), org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            List<Map<String, Object>> events = invocation.getArgument(0);
            int received = events.size();
            int rejected = 0;
            String reason = "Rejected event";
            for (Map<String, Object> event : events) {
                if (event.containsKey("garbage")) {
                    rejected++;
                } else if (!event.containsKey("schemaVersion") || !Integer.valueOf(1).equals(event.get("schemaVersion"))) {
                    rejected++;
                    reason = "schemaVersion missing or invalid";
                }
            }
            int accepted = received - rejected;
            String status = rejected == received ? "REJECTED" : (rejected > 0 ? "PARTIAL" : "SUCCESS");

            return BatchIngestionResponse.builder()
                .status(status)
                .totalReceived(received)
                .totalAccepted(accepted)
                .totalRejected(rejected)
                .rejectedReasons(rejected == 0 ? List.of() : List.of(reason))
                .build();
        });
    }

    @Test
    @DisplayName("POST /api/v1/ingestion/tickets returns 201 when the batch is successful")
    void ticketsBatchSuccess() throws Exception {
        when(ingestionService.ingest(anyList(), eq(SourceType.TICKETING)))
                .thenReturn(response("SUCCESS", 1, 1, 0));

        mockMvc.perform(post("/api/v1/ingestion/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(Map.of(
                                "timestamp", "2026-05-05T11:00:00Z",
                                "userId", "user-1",
                                "status", "validated"
                        )))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalAccepted").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/ingestion/payments returns 207 when the batch is partial")
    void paymentsBatchPartial() throws Exception {
        when(ingestionService.ingest(anyList(), eq(SourceType.PAYMENT)))
                .thenReturn(response("PARTIAL", 3, 2, 1));

        mockMvc.perform(post("/api/v1/ingestion/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(
                                Map.of("timestamp", "2026-05-05T11:00:00Z", "transactionId", "tx-1", "status", "completed"),
                                Map.of("garbage", "data"),
                                Map.of("timestamp", "2026-05-05T11:01:00Z", "transactionId", "tx-2", "status", "completed")
                        ))))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.totalRejected").value(1));
    }

    // =========================================================================
    // Helper factories for each source type
    // =========================================================================

    private Map<String, Object> ticketEvent(String line) {
        return Map.of(
                "schemaVersion", 1,
                "timestamp", validTs(),
                "userId", "user-" + line,
                "status", "validated",
                "line", line,
                "stationId", "ST-01"
        );
    }

    private Map<String, Object> paymentEvent(String method) {
        return Map.of(
                "schemaVersion", 1,
                "timestamp", validTs(),
                "transactionId", "tx-" + method,
                "amount", 25.50,
                "status", "completed",
                "paymentMethod", method,
                "paymentType", "TICKET"
        );
    }

    private Map<String, Object> vehicleEvent(String vehicleId) {
        return Map.of(
                "schemaVersion", 1,
                "timestamp", validTs(),
                "vehicleId", vehicleId,
                "status", "in_service",
                "line", "L1",
                "avgSpeed", 60
        );
    }

    private Map<String, Object> incidentEvent(double latitude, double longitude) {
        return Map.of(
                "schemaVersion", 1,
                "timestamp", validTs(),
                "incidentId", "inc-" + latitude + "-" + longitude,
                "type", "delay",
                "severity", "LOW",
                "latitude", latitude,
                "longitude", longitude
        );
    }

    private Map<String, Object> subscriptionEvent(String action) {
        return Map.of(
                "schemaVersion", 1,
                "timestamp", validTs(),
                "userId", "sub-user-1",
                "action", action,
                "subscriptionType", "monthly"
        );
    }

    private Map<String, Object> userEvent(String action) {
        return Map.of(
                "schemaVersion", 1,
                "timestamp", validTs(),
                "userId", "u-001",
                "action", action
        );
    }

    /** Returns a valid ISO-8601 timestamp string. */
    private String validTs() {
        return Instant.now().toString();
    }

    /** An event guaranteed to fail validation – no timestamp field. */
    private Map<String, Object> invalidEvent() {
        return Map.of("garbage", "data");
    }

    private Map<String, Object> missingSchemaVersionEvent() {
        return Map.of(
                "timestamp", validTs(),
                "userId", "u-no-schema",
                "action", "active"
        );
    }

    private Map<String, Object> wrongSchemaVersionEvent() {
        return Map.of(
                "schemaVersion", 99,
                "timestamp", validTs(),
                "userId", "u-wrong-schema",
                "action", "active"
        );
    }

    // =========================================================================
    // /api/v1/ingestion/tickets
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ingestion/tickets")
    class TicketsEndpoint {

        private static final String URL = "/api/v1/ingestion/tickets";

        @Test
        @DisplayName("A – single valid ticket event → 201 SUCCESS")
        void singleValidTicket() throws Exception {
            String body = objectMapper.writeValueAsString(List.of(ticketEvent("L1")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.totalAccepted").value(1))
                    .andExpect(jsonPath("$.totalRejected").value(0));
        }

        @Test
        @DisplayName("B – three valid ticket events → 201 SUCCESS, all accepted")
        void multiValidTickets() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(ticketEvent("L1"), ticketEvent("L2"), ticketEvent("L3")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.totalReceived").value(3))
                    .andExpect(jsonPath("$.totalAccepted").value(3));
        }

        @Test
        @DisplayName("C – partial batch (2 valid + 1 invalid) → 207 PARTIAL")
        void partialTicketBatch() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(ticketEvent("L1"), invalidEvent(), ticketEvent("L2")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isMultiStatus())
                    .andExpect(jsonPath("$.status").value("PARTIAL"))
                    .andExpect(jsonPath("$.totalAccepted").value(2))
                    .andExpect(jsonPath("$.totalRejected").value(1));
        }

        @Test
        @DisplayName("D – empty batch → 400")
        void emptyTicketBatch() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("[]"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // /api/v1/ingestion/payments
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ingestion/payments")
    class PaymentsEndpoint {

        private static final String URL = "/api/v1/ingestion/payments";

        @Test
        @DisplayName("A – single valid payment event → 201 SUCCESS")
        void singleValidPayment() throws Exception {
            String body = objectMapper.writeValueAsString(List.of(paymentEvent("CARD")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("B – three valid payment events → 201 SUCCESS, all accepted")
        void multiValidPayments() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(paymentEvent("CARD"), paymentEvent("CASH"), paymentEvent("MOBILE")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalAccepted").value(3));
        }

        @Test
        @DisplayName("C – partial batch (2 valid + 1 invalid) → 207 PARTIAL")
        void partialPaymentBatch() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(paymentEvent("CARD"), invalidEvent(), paymentEvent("CASH")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isMultiStatus())
                    .andExpect(jsonPath("$.status").value("PARTIAL"));
        }

        @Test
        @DisplayName("D – empty batch → 400")
        void emptyPaymentBatch() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("[]"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // /api/v1/ingestion/vehicles
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ingestion/vehicles")
    class VehiclesEndpoint {

        private static final String URL = "/api/v1/ingestion/vehicles";

        @Test
        @DisplayName("A – single valid vehicle event → 201 SUCCESS")
        void singleValidVehicle() throws Exception {
            String body = objectMapper.writeValueAsString(List.of(vehicleEvent("V-001")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("B – three valid vehicle events → 201 SUCCESS, all accepted")
        void multiValidVehicles() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(vehicleEvent("V-001"), vehicleEvent("V-002"), vehicleEvent("V-003")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalAccepted").value(3));
        }

        @Test
        @DisplayName("C – partial batch (2 valid + 1 invalid) → 207 PARTIAL")
        void partialVehicleBatch() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(vehicleEvent("V-001"), invalidEvent(), vehicleEvent("V-002")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isMultiStatus())
                    .andExpect(jsonPath("$.status").value("PARTIAL"));
        }

        @Test
        @DisplayName("D – empty batch → 400")
        void emptyVehicleBatch() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("[]"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // /api/v1/ingestion/incidents
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ingestion/incidents")
    class IncidentsEndpoint {

        private static final String URL = "/api/v1/ingestion/incidents";

        @Test
        @DisplayName("A – single valid incident event → 201 SUCCESS")
        void singleValidIncident() throws Exception {
            String body = objectMapper.writeValueAsString(List.of(incidentEvent(33.5731, -7.5898)));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("B – three valid incident events → 201 SUCCESS, all accepted")
        void multiValidIncidents() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(
                            incidentEvent(33.5731, -7.5898),
                            incidentEvent(33.5800, -7.6000),
                            incidentEvent(33.5900, -7.6100)));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalAccepted").value(3));
        }

        @Test
        @DisplayName("C – partial batch (2 valid + 1 invalid) → 207 PARTIAL")
        void partialIncidentBatch() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(incidentEvent(33.5731, -7.5898), invalidEvent(), incidentEvent(33.5800, -7.6000)));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isMultiStatus())
                    .andExpect(jsonPath("$.status").value("PARTIAL"));
        }

        @Test
        @DisplayName("D – empty batch → 400")
        void emptyIncidentBatch() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("[]"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // /api/v1/ingestion/subscriptions
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ingestion/subscriptions")
    class SubscriptionsEndpoint {

        private static final String URL = "/api/v1/ingestion/subscriptions";

        @Test
        @DisplayName("A – single valid subscription event → 201 SUCCESS")
        void singleValidSubscription() throws Exception {
            String body = objectMapper.writeValueAsString(List.of(subscriptionEvent("created")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("B – three valid subscription events → 201 SUCCESS, all accepted")
        void multiValidSubscriptions() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(subscriptionEvent("created"), subscriptionEvent("renewed"), subscriptionEvent("cancelled")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalAccepted").value(3));
        }

        @Test
        @DisplayName("C – partial batch (2 valid + 1 invalid) → 207 PARTIAL")
        void partialSubscriptionBatch() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(subscriptionEvent("created"), invalidEvent(), subscriptionEvent("renewed")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isMultiStatus())
                    .andExpect(jsonPath("$.status").value("PARTIAL"));
        }

        @Test
        @DisplayName("D – empty batch → 400")
        void emptySubscriptionBatch() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("[]"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // /api/v1/ingestion/users
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ingestion/users")
    class UsersEndpoint {

        private static final String URL = "/api/v1/ingestion/users";

        @Test
        @DisplayName("A – single valid user event → 201 SUCCESS")
        void singleValidUser() throws Exception {
            String body = objectMapper.writeValueAsString(List.of(userEvent("active")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("B – three valid user events → 201 SUCCESS, all accepted")
        void multiValidUsers() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(userEvent("active"), userEvent("inactive"), userEvent("active")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalAccepted").value(3));
        }

        @Test
        @DisplayName("C – partial batch (2 valid + 1 invalid) → 207 PARTIAL")
        void partialUserBatch() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(userEvent("active"), invalidEvent(), userEvent("inactive")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isMultiStatus())
                    .andExpect(jsonPath("$.status").value("PARTIAL"));
        }

        @Test
        @DisplayName("D – empty batch → 400")
        void emptyUserBatch() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("[]"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("POST /api/v1/ingestion/users returns 400 for an empty batch")
    void usersBatchEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    private BatchIngestionResponse response(String status, int received, int accepted, int rejected) {
        return BatchIngestionResponse.builder()
                .status(status)
                .totalReceived(received)
                .totalAccepted(accepted)
                .totalRejected(rejected)
                .rejectedReasons(rejected == 0 ? List.of() : List.of("Rejected event"))
                .build();
    }

    // =========================================================================
    // Schema versioning enforcement
    // =========================================================================

    @Nested
    @DisplayName("Schema versioning")
    class SchemaVersioning {

        private static final String URL = "/api/v1/ingestion/tickets";

        @Test
        @DisplayName("E – missing schemaVersion → all events rejected (400 REJECTED)")
        void missingSchemaVersion_allRejected() throws Exception {
            String body = objectMapper.writeValueAsString(List.of(missingSchemaVersionEvent()));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.totalAccepted").value(0))
                    .andExpect(jsonPath("$.rejectedReasons[0]", containsString("schemaVersion")));
        }

        @Test
        @DisplayName("F – wrong schemaVersion (99) → all events rejected (400 REJECTED)")
        void wrongSchemaVersion_allRejected() throws Exception {
            String body = objectMapper.writeValueAsString(List.of(wrongSchemaVersionEvent()));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.totalAccepted").value(0))
                    .andExpect(jsonPath("$.rejectedReasons[0]", containsString("schemaVersion")));
        }

        @Test
        @DisplayName("G – correct schemaVersion (1) → event accepted (201 SUCCESS)")
        void correctSchemaVersion_accepted() throws Exception {
            String body = objectMapper.writeValueAsString(List.of(ticketEvent("L1")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.totalAccepted").value(1));
        }

        @Test
        @DisplayName("H – mixed batch: 1 valid + 1 missing schemaVersion → 207 PARTIAL")
        void mixedSchemaVersionBatch_partial() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(ticketEvent("L1"), missingSchemaVersionEvent()));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isMultiStatus())
                    .andExpect(jsonPath("$.status").value("PARTIAL"))
                    .andExpect(jsonPath("$.totalAccepted").value(1))
                    .andExpect(jsonPath("$.totalRejected").value(1));
        }
    }
}
