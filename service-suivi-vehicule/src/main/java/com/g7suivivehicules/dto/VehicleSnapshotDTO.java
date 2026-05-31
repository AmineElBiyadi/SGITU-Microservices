package com.g7suivivehicules.dto;

import com.g7suivivehicules.entity.Vehicule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleSnapshotDTO {
    private UUID id;
    private String immatriculation;
    private Vehicule.TypeVehicule type;
    private String ligne;
    private Vehicule.StatutVehicule statut;
    private UUID conducteurId;

    // Dernière position
    private Double latitude;
    private Double longitude;
    private Double vitesse;
    private Double cap;
    private LocalDateTime timestamp;

    // Alertes actives
    private List<AlertResponseDTO> alertesActives;
}
