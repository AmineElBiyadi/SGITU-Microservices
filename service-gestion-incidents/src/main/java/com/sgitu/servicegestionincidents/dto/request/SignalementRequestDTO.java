package com.sgitu.servicegestionincidents.dto.request;

import com.sgitu.servicegestionincidents.model.enums.TypeIncident;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementRequestDTO {

    @NotNull(message = "Le type d'incident est obligatoire")
    private TypeIncident type;

    @NotBlank(message = "La description est obligatoire")
    @Size(min = 10, max = 1000, message = "La description doit contenir entre 10 et 1000 caractères")
    private String description;

    @NotNull(message = "La date de l'incident est obligatoire")
    private LocalDateTime dateIncident;

    @NotNull(message = "La latitude est obligatoire")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    private Double longitude;

    private List<PreuveDTO> preuves;

    @NotNull(message = "L'ID du déclarant est obligatoire")
    private Long declarantId;

    private String vehiculeId;

    private String ligneTransport;

    // Rempli par le controller à partir du JWT (VOYAGEUR, CONDUCTEUR, etc.)
    private String role;
}
