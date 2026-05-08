package com.sgitu.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Format standard des réponses d'erreur renvoyées par GlobalExceptionHandler.
 */
@Schema(description = "Format standard des erreurs retournées par l'API")
public class ErrorResponseDTO {

    @Schema(description = "Horodatage de l'erreur", example = "2026-05-08T10:00:00.000000")
    public String timestamp;

    @Schema(description = "Code HTTP", example = "404")
    public int status;

    @Schema(description = "Libellé HTTP", example = "Not Found")
    public String error;

    @Schema(description = "Message détaillé", example = "Utilisateur introuvable avec l'id : 99")
    public String message;

    @Schema(description = "Chemin de la requête", example = "/api/users/99")
    public String path;
}

