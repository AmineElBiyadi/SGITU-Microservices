package com.sgitu.userservice.controller;

import com.sgitu.userservice.dto.*;
import com.sgitu.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Gestion des Utilisateurs", description = """
    Endpoints pour la création, la modification et la gestion des profils utilisateurs.
    La plupart des opérations nécessitent un token JWT Bearer (obtenu via POST /auth/login).
    """)
public class UserController {

    private final UserService userService;

    // ── POST /users — Créer un utilisateur (Public) ──

    @Operation(
        summary = "Créer un nouvel utilisateur",
        description = """
            Crée un compte utilisateur avec son profil et son rôle initial.
            Cet endpoint est **public** — aucun token requis.
            Il est également appelé par G10 lors de l'inscription.

            **Rôles disponibles :** ROLE_PASSENGER, ROLE_STUDENT, ROLE_DRIVER,
            ROLE_STAFF, ROLE_OPERATOR, ROLE_TECHNICIAN, ROLE_ADMIN
            """,
        security = {}   // public
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides (champ obligatoire manquant, email mal formaté, mot de passe < 8 caractères, rôle introuvable)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(value = """
                    {"timestamp":"2026-05-08T10:00:00","status":400,"error":"Bad Request","message":"Le mot de passe doit contenir au moins 8 caractères","path":"/api/users"}
                    """))),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé par un autre compte",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(value = """
                    {"timestamp":"2026-05-08T10:00:00","status":409,"error":"Conflict","message":"Un compte avec cet email existe déjà : jean@example.com","path":"/api/users"}
                    """)))
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request) {
        UserResponseDTO created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /users — Lister les utilisateurs (Admin) ──

    @Operation(
        summary = "Lister les utilisateurs",
        description = """
            Retourne la liste de tous les utilisateurs.
            Paramètre optionnel `role` pour filtrer (ex: `ROLE_DRIVER`).
            **Réservé aux administrateurs (ROLE_ADMIN).**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste retournée avec succès",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = UserResponseDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Rôle ROLE_ADMIN requis",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(
            @Parameter(description = "Filtrer par rôle (ex: ROLE_DRIVER, ROLE_PASSENGER)",
                example = "ROLE_DRIVER")
            @RequestParam(required = false) String role) {
        if (role != null && !role.isBlank()) {
            return ResponseEntity.ok(userService.getUsersByRole(role));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ── GET /users/{id} — Récupérer un profil (Authentifié) ──

    @Operation(
        summary = "Récupérer un utilisateur par son ID",
        description = "Retourne le profil complet d'un utilisateur. Nécessite un token JWT valide.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur trouvé",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Aucun utilisateur avec cet ID",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(value = """
                    {"timestamp":"2026-05-08T10:00:00","status":404,"error":"Not Found","message":"Utilisateur introuvable avec l'id : 99","path":"/api/users/99"}
                    """)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "ID de l'utilisateur", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // ── GET /users/{id}/exists — Vérifier existence (Authentifié) ──

    @Operation(
        summary = "Vérifier si un utilisateur existe",
        description = "Endpoint léger utilisé par les autres microservices pour vérifier l'existence d'un utilisateur sans charger son profil complet.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Résultat de la vérification",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(example = "{\"exists\": true}"))),
        @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}/exists")
    public ResponseEntity<Map<String, Boolean>> userExists(
            @Parameter(description = "ID de l'utilisateur à vérifier", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(Map.of("exists", userService.userExists(id)));
    }

    // ── PUT /users/{id} — Modifier un profil (Propriétaire) ──

    @Operation(
        summary = "Mettre à jour le profil d'un utilisateur",
        description = """
            Modifie les informations du profil (nom, téléphone, adresse, date de naissance) et/ou l'email.
            Le champ `password` est optionnel : s'il est fourni, le mot de passe est mis à jour.
            Pour changer uniquement le mot de passe, préférer `PUT /users/{id}/password`.
            Nécessite un token JWT valide (propriétaire du compte ou admin).
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil mis à jour",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Email invalide ou mot de passe trop court (< 8 caractères)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "409", description = "Nouvel email déjà utilisé par un autre compte",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "ID de l'utilisateur à modifier", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // ── PUT /users/{id}/password — Changer le mot de passe (Propriétaire) ──

    @Operation(
        summary = "Changer le mot de passe",
        description = """
            Remplace le mot de passe de l'utilisateur par un nouveau.
            Le corps doit contenir le champ `newPassword` (ou `password`).
            Le nouveau mot de passe doit comporter au moins 8 caractères.
            Nécessite un token JWT valide.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mot de passe modifié avec succès (corps vide)"),
        @ApiResponse(responseCode = "400", description = "Champ newPassword absent ou vide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(value = """
                    {"timestamp":"2026-05-08T10:00:00","status":400,"error":"Bad Request","message":"Le nouveau mot de passe est obligatoire (champ 'newPassword')","path":"/api/users/1/password"}
                    """))),
        @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "ID de l'utilisateur", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Nouveau mot de passe",
                required = true,
                content = @Content(schema = @Schema(example = "{\"newPassword\": \"NouveauPass123!\"}"))
            )
            @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            newPassword = body.get("password");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe est obligatoire (champ 'newPassword')");
        }
        userService.changePassword(id, newPassword);
        return ResponseEntity.ok().build();
    }

    // ── PUT /users/{id}/roles — Modifier les rôles (Admin) ──

    @Operation(
        summary = "Modifier les rôles d'un utilisateur",
        description = """
            Remplace l'ensemble des rôles de l'utilisateur par la liste fournie.
            **Réservé aux administrateurs (ROLE_ADMIN).**

            Rôles valides : ROLE_PASSENGER, ROLE_STUDENT, ROLE_DRIVER, ROLE_STAFF,
            ROLE_OPERATOR, ROLE_TECHNICIAN, ROLE_ADMIN
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rôles mis à jour",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Liste de rôles vide ou rôle introuvable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Rôle ROLE_ADMIN requis",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}/roles")
    public ResponseEntity<UserResponseDTO> updateRoles(
            @Parameter(description = "ID de l'utilisateur", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Liste des nouveaux rôles",
                content = @Content(schema = @Schema(example = "{\"roles\": [\"ROLE_PASSENGER\", \"ROLE_STUDENT\"]}"))
            )
            @RequestBody Map<String, List<String>> body) {
        List<String> roles = body.get("roles");
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("La liste des rôles est obligatoire");
        }
        return ResponseEntity.ok(userService.updateRoles(id, roles));
    }

    // ── PUT /users/{id}/deactivate — Désactiver un compte (Admin) ──

    @Operation(
        summary = "Désactiver un compte utilisateur",
        description = """
            Passe le statut du compte à **inactif**. L'utilisateur ne pourra plus se connecter.
            Un événement est envoyé à G8 (Analytics).
            **Réservé aux administrateurs (ROLE_ADMIN).**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Compte désactivé",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Rôle ROLE_ADMIN requis",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<UserResponseDTO> deactivateUser(
            @Parameter(description = "ID de l'utilisateur à désactiver", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    // ── PUT /users/{id}/activate — Réactiver un compte (Admin) ──

    @Operation(
        summary = "Réactiver un compte utilisateur",
        description = """
            Remet un compte désactivé à l'état **actif**.
            Un événement est envoyé à G8 (Analytics).
            **Réservé aux administrateurs (ROLE_ADMIN).**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Compte réactivé",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Rôle ROLE_ADMIN requis",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}/activate")
    public ResponseEntity<UserResponseDTO> activateUser(
            @Parameter(description = "ID de l'utilisateur à réactiver", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    // ── DELETE /users/{id} — Supprimer un compte (Admin) ──

    @Operation(
        summary = "Supprimer définitivement un utilisateur",
        description = """
            Supprime l'utilisateur et son profil de la base de données. Action **irréversible**.
            **Réservé aux administrateurs (ROLE_ADMIN).**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Utilisateur supprimé (corps vide)"),
        @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Rôle ROLE_ADMIN requis",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID de l'utilisateur à supprimer", example = "1", required = true)
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
