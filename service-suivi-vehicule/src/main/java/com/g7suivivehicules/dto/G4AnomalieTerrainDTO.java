package com.g7suivivehicules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * DTO pour l'envoi d'anomalies terrain au Groupe 4 via Kafka.
 */
@Data
@Builder
@Schema(description = "Alerte d'anomalie terrain envoyée à G4 (Coordination)")
public class G4AnomalieTerrainDTO {

    @Schema(description = "Identifiant du véhicule concerné", example = "V-123")
    private String vehiculeId;

    @Schema(description = "Type d'anomalie", example = "RETARD", allowableValues = {"RETARD", "DEVIATION", "PANNE", "ALERTE"})
    private String type;

    @Schema(description = "Détails de l'anomalie", example = "Véhicule immobilisé hors arrêt")
    private String details;

    @Schema(description = "Horodatage de l'alerte (ISO-8601)", example = "2026-05-06T15:45:00Z")
    private String timestamp; 
}
