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
public class RenouvellementRequestDTO {

    @NotNull(message = "L'ID de l'abonnement est obligatoire")
    private Long abonnementId;
}
