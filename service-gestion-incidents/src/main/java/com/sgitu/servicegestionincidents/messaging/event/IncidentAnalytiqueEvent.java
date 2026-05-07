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
public class IncidentAnalytiqueEvent implements Serializable {

    private String reference;
    
    private String source;          // USER, IOT
    private String type;            // PANNE_VEHICULE, RETARD, ACCIDENT, etc.
    private String gravite;         // FAIBLE, MOYEN, ELEVE, CRITIQUE
    private String statut;          // CLOTURE, ANNULE
    
    private String vehiculeId;
    private String ligneTransport;
    private Long declarantId;
    private Long responsableId;
    
    private String description;
    private Double latitude;
    private Double longitude;
    
    private LocalDateTime dateSignalement;
    private LocalDateTime dateIncident;
    private LocalDateTime dateResolution;
    private LocalDateTime dateLimiteResolution;
}
