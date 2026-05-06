package com.serviceabonnement.dto.response;

import com.serviceabonnement.enums.CategorieAbonnement;
import com.serviceabonnement.enums.DureeOffre;
import com.serviceabonnement.enums.MoyenTransport;
import com.serviceabonnement.enums.StatutOffre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanAbonnementResponseDTO {
    private Long id;
    private String nom;
    private String description;
    private BigDecimal prix;
    private DureeOffre duree;
    private CategorieAbonnement categorie;
    private MoyenTransport moyenTransport;
    private StatutOffre statut;
}
