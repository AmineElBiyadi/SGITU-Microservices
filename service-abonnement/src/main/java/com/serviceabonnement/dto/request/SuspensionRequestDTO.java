package com.serviceabonnement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspensionRequestDTO {

    @NotNull(message = "L'ID de l'abonnement est obligatoire")
    private Long abonnementId;

    @NotBlank(message = "Le motif est obligatoire")
    private String motif;
}
