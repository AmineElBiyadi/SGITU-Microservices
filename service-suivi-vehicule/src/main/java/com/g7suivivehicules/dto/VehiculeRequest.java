package com.g7suivivehicules.dto;

import com.g7suivivehicules.entity.Vehicule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculeRequest {

    @NotBlank(message = "immatriculation obligatoire")
    private String immatriculation;

    @NotNull(message = "type obligatoire")
    private Vehicule.TypeVehicule type; // BUS ou TRAM

    private String ligne;

    private UUID conducteurId;
}