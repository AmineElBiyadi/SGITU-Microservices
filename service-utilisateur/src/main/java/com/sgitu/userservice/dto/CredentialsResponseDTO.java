package com.sgitu.userservice.dto;

import lombok.*;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Données d'authentification (Usage interne API Gateway)")
public class CredentialsResponseDTO {

    @Schema(description = "ID utilisateur")
    private Long id;

    @Schema(description = "Email utilisateur")
    private String email;

    @Schema(description = "Hash BCrypt du mot de passe")
    private String passwordHash;

    @Schema(description = "Liste des rôles")
    private List<String> roles;

    @Schema(description = "État du compte")
    private Boolean active;
}
