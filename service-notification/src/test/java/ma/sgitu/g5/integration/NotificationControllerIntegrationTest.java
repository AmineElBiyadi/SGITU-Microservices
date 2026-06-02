package ma.sgitu.g5.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.sgitu.g5.repository.NotificationRepository;
import ma.sgitu.g5.support.GatewayAuthHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Tests d'intégration — API REST NotificationController")
class NotificationControllerIntegrationTest extends AbstractG5IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDb() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /health — public, sans auth")
    void health_public() throws Exception {
        mockMvc.perform(get("/api/notifications/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("POST /send LOG — canal admin sans provider externe")
    void send_logChannel() throws Exception {
        String notifId = "integ-log-" + UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "notificationId", notifId,
                "sourceService", "G7_SUIVI_VEHICULES",
                "eventType", "LOG_ALERT_ADMIN",
                "channel", "LOG",
                "priority", "HIGH",
                "recipient", Map.of("userId", "admin-1"),
                "metadata", Map.of("data", Map.of(
                        "logLevel", "ERROR",
                        "serviceName", "G7",
                        "message", "Anomalie GPS"
                ))
        );

        mockMvc.perform(GatewayAuthHeaders.withUser(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("QUEUED")))
                .andExpect(jsonPath("$.notificationId", is(notifId)));

        Thread.sleep(500);

        mockMvc.perform(GatewayAuthHeaders.withUser(get("/api/notifications/" + notifId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceService", is("G7_SUIVI_VEHICULES")));
    }

    @Test
    @DisplayName("POST /send — idempotence ALREADY_QUEUED")
    void send_idempotence() throws Exception {
        String notifId = "integ-dup-" + UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "notificationId", notifId,
                "sourceService", "G4_COORDINATION",
                "eventType", "MISSION_CREATED",
                "channel", "LOG",
                "priority", "NORMAL",
                "recipient", Map.of("userId", "driver-1"),
                "metadata", Map.of("data", Map.of("missionCode", "M-1"))
        );
        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(GatewayAuthHeaders.withUser(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON).content(json)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("QUEUED")));

        mockMvc.perform(GatewayAuthHeaders.withUser(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON).content(json)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("ALREADY_QUEUED")));
    }

    @Test
    @DisplayName("GET /api/notifications sans auth → 403")
    void list_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /send EMAIL sans email → 400")
    void send_emailMissingRecipient() throws Exception {
        Map<String, Object> body = Map.of(
                "notificationId", UUID.randomUUID().toString(),
                "sourceService", "G6_PAYMENT",
                "eventType", "PAYMENT_SUCCESS",
                "channel", "EMAIL",
                "priority", "NORMAL",
                "recipient", Map.of("userId", "u-1")
        );

        mockMvc.perform(GatewayAuthHeaders.withUser(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }
}
