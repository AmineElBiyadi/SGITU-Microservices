package com.serviceabonnement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Structure de réponse standard pour les erreurs")
public class ErrorResponseDTO {

    @Schema(description = "Horodatage de l'erreur", example = "2026-05-07T13:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "Code de statut HTTP", example = "404")
    private int status;

    @Schema(description = "Libellé de l'erreur HTTP", example = "Not Found")
    private String error;

    @Schema(description = "Message d'erreur compréhensible par l'utilisateur", example = "Ressource non trouvée")
    private String message;

    @Schema(description = "Nom du service ayant généré l'erreur", example = "service-abonnement")
    private String serviceName;
}
