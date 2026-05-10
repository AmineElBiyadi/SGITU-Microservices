package com.g7suivivehicules.dto;

import com.g7suivivehicules.entity.Vehicule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Données pour créer ou modifier un véhicule")
public class VehiculeRequest {

    @NotBlank(message = "immatriculation obligatoire")
    @Schema(description = "Plaque d'immatriculation unique du véhicule", example = "BUS-G4-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String immatriculation;

    @NotNull(message = "type obligatoire")
    @Schema(description = "Type de véhicule", example = "BUS", allowableValues = {"BUS", "TRAM", "TAXI", "METRO", "TRAIN"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private Vehicule.TypeVehicule type;

    @Schema(description = "Identifiant de la ligne desservie (référence G4)", example = "G4")
    private String ligne;

    @Schema(description = "UUID du conducteur assigné (référence externe)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID conducteurId;
}