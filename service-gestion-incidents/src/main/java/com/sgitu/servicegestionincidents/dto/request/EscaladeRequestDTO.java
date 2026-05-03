package com.sgitu.servicegestionincidents.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscaladeRequestDTO {

    @NotBlank(message = "Le motif d'escalade est obligatoire")
    private String motif;
}
