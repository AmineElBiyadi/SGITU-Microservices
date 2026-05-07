package com.g7suivivehicules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Données de position GPS émises par le capteur IoT du véhicule")
public class PositionGPSRequest {

    @NotNull(message = "vehiculeId obligatoire")
    @Schema(description = "UUID du véhicule émetteur", example = "53c31262-591a-44d4-8872-51e84611ac5e", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID vehiculeId;

    @NotNull(message = "latitude obligatoire")
    @Schema(description = "Latitude GPS (décimal, entre -90 et 90)", example = "36.7372", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitude;

    @NotNull(message = "longitude obligatoire")
    @Schema(description = "Longitude GPS (décimal, entre -180 et 180)", example = "3.0865", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitude;

    @Schema(description = "Vitesse instantanée en km/h", example = "85.0")
    private Double vitesse;

    @Schema(description = "Cap de déplacement en degrés (0 = Nord, 90 = Est, 180 = Sud, 270 = Ouest)", example = "90.0")
    private Double cap;
}