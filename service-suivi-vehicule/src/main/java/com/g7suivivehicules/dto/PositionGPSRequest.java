package com.g7suivivehicules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionGPSRequest {

    @NotNull(message = "L'ID du véhicule est obligatoire")
    @Schema(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID vehiculeId;

    @NotNull(message = "latitude obligatoire")
    @Min(value = -90, message = "Coordonnées GPS hors limites (latitude doit être >= -90)")
    @Max(value = 90, message = "Coordonnées GPS hors limites (latitude doit être <= 90)")
    @Schema(description = "Latitude GPS (décimal, entre -90 et 90)", example = "36.7372", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitude;

    @NotNull(message = "longitude obligatoire")
    @Min(value = -180, message = "Coordonnées GPS hors limites (longitude doit être >= -180)")
    @Max(value = 180, message = "Coordonnées GPS hors limites (longitude doit être <= 180)")
    @Schema(description = "Longitude GPS (décimal, entre -180 et 180)", example = "3.0865", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitude;

    @Schema(description = "Vitesse en km/h", example = "45.5")
    private Double vitesse;

    @Schema(description = "Cap de direction en degrés (0-360)", example = "180.0")
    private Double cap;
}