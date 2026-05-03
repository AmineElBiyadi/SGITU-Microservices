package com.sgitu.servicegestionincidents.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClotureRequestDTO {

    @NotBlank(message = "Le motif de clôture est obligatoire")
    private String motif;
}
