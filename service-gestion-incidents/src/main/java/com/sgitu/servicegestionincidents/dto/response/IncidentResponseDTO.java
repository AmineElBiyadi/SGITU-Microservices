package com.sgitu.servicegestionincidents.dto.response;

import com.sgitu.servicegestionincidents.model.enums.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentResponseDTO {

    private Long id;
    private String reference;
    private TypeIncident type;
    private String description;
    private LocalDateTime dateSignalement;
    private LocalDateTime dateIncident;
    private StatutIncident statut;
    private NiveauGravite gravite;
    private Long declarantId;
    private Long responsableId;
    private String adresse;
    private Double latitude;
    private Double longitude;
}
