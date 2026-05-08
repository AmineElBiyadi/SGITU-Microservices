package com.sgitu.servicegestionincidents.dto.request;

import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatutUpdateRequestDTO {

    @NotNull(message = "Le statut est obligatoire")
    private StatutIncident statut;
}
