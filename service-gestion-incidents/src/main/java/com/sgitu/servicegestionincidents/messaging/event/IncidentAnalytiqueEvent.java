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

    private Long incidentId;
    private String reference;
    private String type;            // PANNE_VEHICULE, RETARD, ACCIDENT, etc.
    private String gravite;         // FAIBLE, MOYEN, ELEVE, CRITIQUE
    private String statut;          // NOUVEAU, EN_COURS, RESOLU, etc.
    private String description;
    private Double latitude;
    private Double longitude;
    private LocalDateTime dateIncident;
    private LocalDateTime dateSignalement;
    private LocalDateTime dateResolution;
    private Long declarantId;
    private Long responsableId;
}
