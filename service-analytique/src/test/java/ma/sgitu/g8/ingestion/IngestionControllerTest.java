package ma.sgitu.g8.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.sgitu.g8.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link IngestionController}.
 *
 * Each of the 6 endpoints is exercised with four scenarios:
 *   A – valid single-event batch   → 201 SUCCESS
 *   B – valid multi-event batch    → 201 SUCCESS
 *   C – partial batch              → 207 PARTIAL
 *   D – empty batch                → 400
 *
 * The tests connect to the Docker MongoDB (localhost:27017/g8_analytics_test)
 * and clear the incoming_events collection before each test to ensure isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    /** A valid ISO-8601 timestamp slightly in the past (no timezone drift issues). */
    private static String validTs() {
        return OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @BeforeEach
    void cleanDb() {
        eventRepository.deleteAll();
    }

    // =========================================================================
    // Helper factories for each source type
    // =========================================================================

    private Map<String, Object> ticketEvent(String line) {
        return Map.of(
                "timestamp", validTs(),
                "userId", "user-" + line,
                "status", "validated",
                "line", line,
                "stationId", "ST-01"
        );
    }

    private Map<String, Object> paymentEvent(String method) {
        return Map.of(
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
                "timestamp", validTs(),
                "vehicleId", vehicleId,
                "status", "in_service",
                "line", "L1",
                "avgSpeed", 60
        );
    }

    private Map<String, Object> incidentEvent(String zone) {
        return Map.of(
                "timestamp", validTs(),
                "incidentId", "inc-" + zone,
                "type", "delay",
                "severity", "LOW",
                "zone", zone
        );
    }

    private Map<String, Object> subscriptionEvent(String action) {
        return Map.of(
                "timestamp", validTs(),
                "userId", "sub-user-1",
                "action", action,
                "subscriptionType", "monthly"
        );
    }

    private Map<String, Object> userEvent(String action) {
        return Map.of(
                "timestamp", validTs(),
                "userId", "u-001",
                "action", action
        );
    }

    /** An event guaranteed to fail validation – no timestamp field. */
    private Map<String, Object> invalidEvent() {
        return Map.of("garbage", "data");
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
            String body = objectMapper.writeValueAsString(List.of(incidentEvent("Z1")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("B – three valid incident events → 201 SUCCESS, all accepted")
        void multiValidIncidents() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(incidentEvent("Z1"), incidentEvent("Z2"), incidentEvent("Z3")));
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalAccepted").value(3));
        }

        @Test
        @DisplayName("C – partial batch (2 valid + 1 invalid) → 207 PARTIAL")
        void partialIncidentBatch() throws Exception {
            String body = objectMapper.writeValueAsString(
                    List.of(incidentEvent("Z1"), invalidEvent(), incidentEvent("Z2")));
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
}
