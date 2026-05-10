package com.g7suivivehicules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO pour l'envoi de notifications au Groupe 5 via REST.
 * Respecte le contrat G5 v3.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de notification pour G5 (PUSH conducteur)")
public class G5NotificationRequest {

    @Schema(description = "Identifiant unique de la notification (UUID)", example = "uuid-g7-temp-001")
    private String notificationId;

    @Schema(description = "Service source", example = "G7_SUIVI_VEHICULES")
    private final String sourceService = "G7_SUIVI_VEHICULES";

    @Schema(description = "Type d'événement (RACINE)", example = "VEHICULE_ALERTE_CONDUCTEUR")
    private String eventType;

    @Schema(description = "Canal de diffusion", example = "PUSH")
    private final String channel = "PUSH";

    @Schema(description = "Niveau de priorité", example = "HIGH", allowableValues = {"HIGH", "NORMAL", "LOW"})
    private String priority;

    private Recipient recipient;

    @Schema(description = "Données brutes pour le template (SANS eventType)")
    private Map<String, String> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recipient {
        @Schema(description = "Identifiant de l'utilisateur (conducteur)", example = "conducteur-V-1042")
        private String userId;

        @Schema(description = "Token du device pour le PUSH", example = "token-device-conducteur")
        private String deviceToken;
    }
}
