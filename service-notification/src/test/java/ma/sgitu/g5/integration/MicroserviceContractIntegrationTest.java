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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simule les contrats REST des microservices partenaires (G4, G6, G8, G10)
 * et vérifie que G5 accepte chaque payload sourceService.
 */
@DisplayName("Tests d'intégration — Contrats inter-microservices (REST)")
class MicroserviceContractIntegrationTest extends AbstractG5IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDb() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("G4 Coordination Transport — MISSION_CANCELLED (contrat G5NotificationWireAdapter)")
    void g4_missionCancelled() throws Exception {
        sendAndExpectQueued(Map.of(
                "notificationId", "g4-" + UUID.randomUUID(),
                "sourceService", "G4_COORDINATION",
                "eventType", "MISSION_CANCELLED",
                "channel", "LOG",
                "priority", "NORMAL",
                "recipient", Map.of("userId", "driver-42", "email", "driver@sgitu.ma"),
                "metadata", Map.of(
                        "lineId", "L-12",
                        "reason", "Panne",
                        "variables", Map.of("missionCode", "M-9988", "reason", "Panne", "cancelSource", "G4")
                )
        ));
    }

    @Test
    @DisplayName("G6 Paiement — PAYMENT_SUCCESS")
    void g6_paymentSuccess() throws Exception {
        sendAndExpectQueued(Map.of(
                "notificationId", "g6-" + UUID.randomUUID(),
                "sourceService", "G6_PAYMENT",
                "eventType", "PAYMENT_SUCCESS",
                "channel", "LOG",
                "priority", "HIGH",
                "recipient", Map.of("userId", "client-1", "email", "client@sgitu.ma"),
                "metadata", Map.of("data", Map.of(
                        "amount", "150",
                        "paymentMethod", "CMI",
                        "invoiceNumber", "INV-2026-001"
                ))
        ));
    }

    @Test
    @DisplayName("G8 Analytique — PUNCTUALITY_ALERT")
    void g8_punctualityAlert() throws Exception {
        sendAndExpectQueued(Map.of(
                "notificationId", "g8-" + UUID.randomUUID(),
                "sourceService", "G8_ANALYTIQUE",
                "eventType", "PUNCTUALITY_ALERT",
                "channel", "LOG",
                "priority", "HIGH",
                "recipient", Map.of("userId", "ops-admin", "email", "ops@sgitu.ma"),
                "metadata", Map.of("data", Map.of(
                        "lineId", "L-5",
                        "value", "62",
                        "threshold", "80",
                        "period", "2026-06-01"
                ))
        ));
    }

    @Test
    @DisplayName("G10 Auth — VERIFY_EMAIL (via Gateway)")
    void g10_verifyEmail() throws Exception {
        sendAndExpectQueued(Map.of(
                "notificationId", "g10-" + UUID.randomUUID(),
                "sourceService", "G10_AUTH",
                "eventType", "VERIFY_EMAIL",
                "channel", "LOG",
                "priority", "NORMAL",
                "recipient", Map.of("userId", "new-user", "email", "new@sgitu.ma"),
                "metadata", Map.of("data", Map.of(
                        "verificationLink", "http://localhost:8080/auth/verify-email?token=abc"
                ))
        ));
    }

    @Test
    @DisplayName("G9 Incidents — INCIDENT_CONFIRMATION (REST fallback)")
    void g9_incidentConfirmation() throws Exception {
        sendAndExpectQueued(Map.of(
                "notificationId", "g9-" + UUID.randomUUID(),
                "sourceService", "G9_INCIDENT",
                "eventType", "INCIDENT_CONFIRMATION",
                "channel", "LOG",
                "priority", "NORMAL",
                "recipient", Map.of("userId", "citizen-1", "email", "citizen@sgitu.ma"),
                "metadata", Map.of("data", Map.of(
                        "reference", "INC-2026-042",
                        "type", "RETARD",
                        "statut", "OUVERT",
                        "lienSuivi", "https://sgitu.ma/suivi/INC-2026-042"
                ))
        ));
    }

    private void sendAndExpectQueued(Map<String, Object> body) throws Exception {
        mockMvc.perform(GatewayAuthHeaders.withUser(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("QUEUED")));
    }
}
