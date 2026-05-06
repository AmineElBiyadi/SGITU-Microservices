package com.g7suivivehicules.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO pour l'envoi du statut des véhicules au Groupe 8 (Analytique).
 */
@Data
@Builder
public class G8VehiculeStatusDTO {
    private String timestamp;      // ISO-8601
    private String vehicleId;      // immatriculation (ex: BUS_404)
    private String status;         // "in_service" ou "out_of_service"
    private String line;
    private Integer delayMinutes;  // dureeMinutes de l'alerte
    private Double speed;
}
