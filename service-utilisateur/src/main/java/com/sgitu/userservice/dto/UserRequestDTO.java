package com.sgitu.userservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Données requises pour créer ou mettre à jour un utilisateur")
public class UserRequestDTO {

    @Email(message = "L'email doit être valide")
    @Schema(description = "Adresse email de l'utilisateur", example = "jean.dupont@example.com")
    private String email;

    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @Schema(description = "Mot de passe (min 8 caractères, requis uniquement à la création)", example = "Secret123!")
    private String password;

    @Schema(description = "Rôle initial attribué (requis uniquement à la création)", example = "ROLE_PASSENGER")
    private String role;

    @Valid
    @Schema(description = "Informations détaillées du profil")
    private ProfileDTO profile;
}
