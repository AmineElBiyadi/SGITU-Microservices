package com.g7suivivehicules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'envoi du statut des véhicules au Groupe 8 (Analytique).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Données analytiques de statut véhicule pour G8")
public class G8VehiculeStatusDTO {

    @Schema(description = "Horodatage de l'événement (ISO-8601)", example = "2026-05-03T11:00:00Z")
    private String timestamp;

    @Schema(description = "Identifiant du véhicule (immatriculation)", example = "BUS_404")
    private String vehicleId;

    @Schema(description = "Statut actuel du véhicule", example = "in_service", allowableValues = {"in_service", "out_of_service"})
    private String status;

    @Schema(description = "Retard accumulé en minutes", example = "2")
    private Integer delayMinutes;

    @Schema(description = "Vitesse actuelle en km/h", example = "45.2")
    private Double speed;
}
