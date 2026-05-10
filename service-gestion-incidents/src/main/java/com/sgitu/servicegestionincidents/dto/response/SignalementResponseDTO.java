package com.sgitu.servicegestionincidents.dto.response;

import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementResponseDTO {

    private Long incidentId;
    private String reference;
    private StatutIncident statut;
    private LocalDateTime dateSignalement;
    private String message;
    private boolean doublon;
}
