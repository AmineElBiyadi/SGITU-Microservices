package com.g7suivivehicules.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO pour l'envoi d'incident au Groupe 9 via Kafka.
 * Format strict exigé par G9.
 */
@Data
@Builder
public class G9IncidentEventDTO {
    private String type;            // Enum strict G9: PANNE_VEHICULE, ACCIDENT, RETARD, ENCOMBREMENT, SECURITE, INFRASTRUCTURE, AUTRE
    private String gravite;         // Enum strict G9: FAIBLE, MOYEN, ELEVE, CRITIQUE
    private String description;
    private Double latitude;
    private Double longitude;
    private String vehiculeId;      // Note: G9 demande Long, on envoie String (immatriculation ou UUID)
    private String ligneTransport;  // Optionnel
    private String dateDetection;   // ISO-8601
}
