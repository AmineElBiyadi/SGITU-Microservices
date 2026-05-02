package com.sgitu.servicegestionincidents.dto.response;

import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.model.enums.TypeAction;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionDTO {

    private Long id;
    private TypeAction type;
    private String description;
    private Long auteurId;
    private LocalDateTime dateAction;
    private StatutIncident ancienStatut;
    private StatutIncident nouveauStatut;
}
