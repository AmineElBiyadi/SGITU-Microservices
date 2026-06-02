package ma.sgitu.g5.service;

import ma.sgitu.g5.dto.request.MetadataDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires — TemplateServiceImpl")
class TemplateServiceImplTest {

    private final TemplateServiceImpl templateService = new TemplateServiceImpl();

    @Test
    @DisplayName("hydrateMessage G1 TICKET_ISSUED avec variables")
    void hydrateMessage_ticketIssued() {
        MetadataDTO meta = new MetadataDTO();
        meta.setData(Map.of(
                "tokenValue", "QR-123",
                "expiresAt", "2026-12-31"
        ));

        String message = templateService.hydrateMessage("TICKET_ISSUED", meta);

        assertThat(message).contains("QR-123").contains("2026-12-31");
    }

    @Test
    @DisplayName("hydrateMessage G4 MISSION_CANCELLED")
    void hydrateMessage_missionCancelled() {
        MetadataDTO meta = new MetadataDTO();
        meta.setData(Map.of(
                "missionCode", "M-42",
                "reason", "Panne véhicule",
                "cancelSource", "G4"
        ));

        String message = templateService.hydrateMessage("MISSION_CANCELLED", meta);

        assertThat(message).contains("M-42").contains("Panne véhicule");
    }

    @Test
    @DisplayName("hydrateMessage eventType inconnu → message générique")
    void hydrateMessage_unknownEvent() {
        String message = templateService.hydrateMessage("UNKNOWN_EVENT_XYZ", null);
        assertThat(message).contains("UNKNOWN_EVENT_XYZ");
    }

    @Test
    @DisplayName("hydrateSubject G6 PAYMENT_SUCCESS")
    void hydrateSubject_paymentSuccess() {
        String subject = templateService.hydrateSubject("PAYMENT_SUCCESS", null);
        assertThat(subject).isEqualTo("SGITU - Paiement validé");
    }

    @Test
    @DisplayName("hydrateSubject sans template → Notification SGITU")
    void hydrateSubject_default() {
        String subject = templateService.hydrateSubject("CUSTOM_EVENT", null);
        assertThat(subject).isEqualTo("Notification SGITU");
    }
}
