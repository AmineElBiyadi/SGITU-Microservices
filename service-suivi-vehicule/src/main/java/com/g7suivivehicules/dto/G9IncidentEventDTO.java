package com.g7suivivehicules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * DTO pour l'envoi d'incident au Groupe 9 via Kafka.
 * Format strict exigé par G9.
 */
@Data
@Builder
@Schema(description = "Événement d'incident envoyé à G9 (Gestion d'Incidents)")
public class G9IncidentEventDTO {

    @Schema(description = "Type de l'incident", example = "PANNE_VEHICULE", allowableValues = {"PANNE_VEHICULE", "ACCIDENT", "RETARD", "ENCOMBREMENT", "SECURITE", "INFRASTRUCTURE", "AUTRE"})
    private String type;

    @Schema(description = "Niveau de gravité", example = "ELEVE", allowableValues = {"FAIBLE", "MOYEN", "ELEVE", "CRITIQUE"})
    private String gravite;

    @Schema(description = "Description détaillée de l'incident", example = "Surchauffe moteur détectée")
    private String description;

    @Schema(description = "Latitude GPS", example = "33.573110")
    private Double latitude;

    @Schema(description = "Longitude GPS", example = "-7.589843")
    private Double longitude;

    @Schema(description = "Identifiant du véhicule concerné", example = "V-1042")
    private String vehiculeId;

    @Schema(description = "Nom ou numéro de la ligne", example = "Ligne 15")
    private String ligneTransport;

    @Schema(description = "Date et heure de détection (ISO-8601)", example = "2026-05-06T14:30:00")
    private String dateDetection;
}
