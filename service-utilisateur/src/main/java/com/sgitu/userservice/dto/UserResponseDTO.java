package com.sgitu.userservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Représentation publique d'un utilisateur")
public class UserResponseDTO {

    @Schema(description = "Identifiant unique de l'utilisateur", example = "1")
    private Long id;

    @Schema(description = "Email de l'utilisateur", example = "jean.dupont@example.com")
    private String email;

    @Schema(description = "État du compte (actif/inactif)", example = "true")
    private Boolean active;

    @Schema(description = "Liste des rôles attribués", example = "[\"ROLE_PASSENGER\"]")
    private List<String> roles;

    @Schema(description = "Détails du profil")
    private ProfileDTO profile;

    @Schema(description = "Date et heure de création")
    private LocalDateTime createdAt;
}
