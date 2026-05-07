package com.serviceabonnement.dto.response;

import com.serviceabonnement.enums.StatutAbonnement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonnementResponseDTO {
    private Long id;
    private Long utilisateurId;
    private PlanAbonnementResponseDTO plan;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private StatutAbonnement statut;
    private Boolean renouvellementAuto;
}
