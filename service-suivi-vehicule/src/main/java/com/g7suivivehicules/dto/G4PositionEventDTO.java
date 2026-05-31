package com.g7suivivehicules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'envoi de la position temps réel au Groupe 4 via Kafka.
 * Format strict exigé par G4.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload de position temps réel envoyé à G4 (Coordination)")
public class G4PositionEventDTO {

    @Schema(description = "Identifiant unique du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
    private String vehiculeId;

    @Schema(description = "Identifiant de la ligne suivie", example = "L-10")
    private String ligneId;

    @Schema(description = "Latitude GPS", example = "35.767")
    private Double lat;

    @com.fasterxml.jackson.annotation.JsonProperty("long")
    @Schema(description = "Longitude GPS", example = "-5.803")
    private Double longitude;

    @Schema(description = "Vitesse instantanée en km/h", example = "42.5")
    private Double vitesse;

    @Schema(description = "Horodatage de la mesure (ISO-8601)", example = "2026-05-06T15:41:00Z")
    private String timestamp; 
}
