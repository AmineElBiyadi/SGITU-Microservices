package com.g7suivivehicules.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO pour l'envoi d'anomalies terrain au Groupe 4 via Kafka.
 */
@Data
@Builder
public class G4AnomalieTerrainDTO {
    private String vehiculeId;
    private String type;        // RETARD / DEVIATION / PANNE
    private String details;
    private String timestamp;   // ISO-8601
}
