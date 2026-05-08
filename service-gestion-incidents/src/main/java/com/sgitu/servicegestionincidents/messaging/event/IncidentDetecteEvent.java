package com.sgitu.servicegestionincidents.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentDetecteEvent implements Serializable {

    private String source;          // "SUIVI_VEHICULE" ou "ANALYTIQUE"
    private String type;            // PANNE_VEHICULE, RETARD, ACCIDENT, etc.
    private String gravite;         // FAIBLE, MOYEN, ELEVE, CRITIQUE
    private String description;
    private Double latitude;
    private Double longitude;
    private Long vehiculeId;
    private String ligneTransport;
    private LocalDateTime dateDetection;
}
