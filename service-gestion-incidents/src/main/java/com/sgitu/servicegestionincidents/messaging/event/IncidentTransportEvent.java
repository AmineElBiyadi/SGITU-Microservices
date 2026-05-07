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
public class IncidentTransportEvent implements Serializable {

    private String referenceIncident;
    
    private String type;            // PANNE_VEHICULE, ACCIDENT, etc.
    private String statut;          // CONFIRME, RESOLU, REJETE
    
    private String vehiculeId;
    private String ligneId;
    
    private String description;
    private Double latitude;
    private Double longitude;
    
    private LocalDateTime timestamp;
}
