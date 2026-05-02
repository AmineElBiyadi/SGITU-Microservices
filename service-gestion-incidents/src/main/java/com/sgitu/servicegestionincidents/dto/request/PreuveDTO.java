package com.sgitu.servicegestionincidents.dto.request;

import com.sgitu.servicegestionincidents.model.enums.TypePreuve;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreuveDTO {

    @NotNull(message = "Le type de preuve est obligatoire")
    private TypePreuve type;

    private String fichierBase64;
    private String description;
}
