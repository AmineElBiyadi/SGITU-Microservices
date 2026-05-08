package com.sgitu.servicegestionincidents.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AffectationRequestDTO {

    @NotNull(message = "L'ID du responsable est obligatoire")
    private Long responsableId;
}
