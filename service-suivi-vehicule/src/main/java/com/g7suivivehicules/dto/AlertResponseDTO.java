package com.g7suivivehicules.dto;

import com.g7suivivehicules.entity.Alert;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AlertResponseDTO {
    private UUID id;
    private UUID vehiculeId;
    private Alert.TypeAlert typeAlert;
    private Alert.Severite severite;
    private Double valeur;
    private Double seuil;
    private Double latitude;
    private Double longitude;
    private String message;
    private LocalDateTime timestampDebut;
    private LocalDateTime timestampFin;
    private Alert.StatutAlert statut;
    private Integer dureeMinutes;

    public static AlertResponseDTO fromEntity(Alert alert) {
        return AlertResponseDTO.builder()
                .id(alert.getId())
                .vehiculeId(alert.getVehiculeId())
                .typeAlert(alert.getTypeAlert())
                .severite(alert.getSeverite())
                .valeur(alert.getValeur())
                .seuil(alert.getSeuil())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .message(alert.getMessage())
                .timestampDebut(alert.getTimestampDebut())
                .timestampFin(alert.getTimestampFin())
                .statut(alert.getStatut())
                .dureeMinutes(alert.getDureeMinutes())
                .build();
    }
}
