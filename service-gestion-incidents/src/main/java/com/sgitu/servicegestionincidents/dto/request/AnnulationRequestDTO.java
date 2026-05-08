package com.sgitu.servicegestionincidents.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnulationRequestDTO {

    @NotBlank(message = "Le motif d'annulation est obligatoire")
    private String motif;
}
