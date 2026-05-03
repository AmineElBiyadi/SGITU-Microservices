package com.sgitu.userservice.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Informations détaillées du profil utilisateur")
public class ProfileDTO {

    @Size(max = 50, message = "Le prénom ne doit pas dépasser 50 caractères")
    @Schema(description = "Prénom", example = "Jean")
    private String firstName;

    @Size(max = 50, message = "Le nom ne doit pas dépasser 50 caractères")
    @Schema(description = "Nom de famille", example = "Dupont")
    private String lastName;

    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    @Schema(description = "Numéro de téléphone", example = "0612345678")
    private String phone;

    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    @Schema(description = "Adresse postale", example = "123 Rue de la Liberté, Casablanca")
    private String address;

    @Past(message = "La date de naissance doit être dans le passé")
    @Schema(description = "Date de naissance")
    private LocalDate birthDate;
}
