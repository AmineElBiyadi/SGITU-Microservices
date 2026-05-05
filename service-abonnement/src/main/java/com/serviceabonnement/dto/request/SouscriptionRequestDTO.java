package com.serviceabonnement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SouscriptionRequestDTO {

    @NotNull(message = "L'ID de l'utilisateur est obligatoire")
    private Long utilisateurId;

    @NotNull(message = "L'ID du plan est obligatoire")
    private Long planId;

    private Boolean renouvellementAuto = true;
}
