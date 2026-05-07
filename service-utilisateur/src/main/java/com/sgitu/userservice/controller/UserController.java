package com.sgitu.userservice.controller;

import com.sgitu.userservice.dto.*;
import com.sgitu.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Gestion des Utilisateurs", description = "Endpoints pour la création, la modification et la gestion des profils utilisateurs")
public class UserController {

    private final UserService userService;

    // ── POST /users — Créer un utilisateur (Public) ──

    @Operation(summary = "Créer un nouvel utilisateur", description = "Permet l'inscription d'un nouvel utilisateur avec son profil et son rôle initial.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données d'entrée invalides"),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé")
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request) {
        UserResponseDTO created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /users/{id} — Récupérer un profil (Authentifié) ──

    @Operation(summary = "Récupérer un utilisateur par son ID", description = "Retourne les détails du profil d'un utilisateur.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "ID de l'utilisateur à récupérer") @PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // ── PUT /users/{id} — Modifier un profil (Propriétaire) ──

    @Operation(summary = "Mettre à jour le profil d'un utilisateur", description = "Permet de modifier les informations personnelles d'un utilisateur.")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "ID de l'utilisateur à modifier") @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // ── PUT /users/{id}/password — Changer le mot de passe (Propriétaire) ──

    @Operation(summary = "Changer le mot de passe", description = "Permet à un utilisateur de modifier son mot de passe de connexion.")
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe est obligatoire");
        }
        userService.changePassword(id, newPassword);
        return ResponseEntity.ok().build();
    }

    // ── PUT /users/{id}/roles — Modifier les rôles (Admin) ──

    @Operation(summary = "Mettre à jour les rôles d'un utilisateur", description = "Action réservée aux administrateurs pour changer les privilèges.")
    @PutMapping("/{id}/roles")
    public ResponseEntity<UserResponseDTO> updateRoles(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long id,
            @RequestBody Map<String, List<String>> body) {
        List<String> roles = body.get("roles");
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("La liste des rôles est obligatoire");
        }
        return ResponseEntity.ok(userService.updateRoles(id, roles));
    }

    // ── PUT /users/{id}/deactivate — Désactiver un compte (Admin) ──

    @Operation(summary = "Désactiver un compte utilisateur", description = "Marque un compte comme inactif (Admin uniquement).")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<UserResponseDTO> deactivateUser(
            @Parameter(description = "ID de l'utilisateur à désactiver") @PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    // ── DELETE /users/{id} — Supprimer un compte (Admin) ──

    @Operation(summary = "Supprimer définitivement un utilisateur", description = "Supprime l'utilisateur et son profil de la base de données (Admin uniquement).")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID de l'utilisateur à supprimer") @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET /users — Lister les utilisateurs (Admin) ──
    // ── GET /users?role=ROLE_X — Filtrer par rôle (Admin) ──

    @Operation(summary = "Lister les utilisateurs", description = "Retourne la liste de tous les utilisateurs, avec possibilité de filtrer par rôle.")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(
            @Parameter(description = "Optionnel : Nom du rôle pour filtrer (ex: ROLE_DRIVER)") 
            @RequestParam(required = false) String role) {
        if (role != null && !role.isBlank()) {
            return ResponseEntity.ok(userService.getUsersByRole(role));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ── GET /users/{id}/exists — Vérifier existence (Authentifié) ──

    @Operation(summary = "Vérifier si un utilisateur existe", description = "Endpoint rapide pour confirmer l'existence d'un utilisateur par son ID.")
    @GetMapping("/{id}/exists")
    public ResponseEntity<Map<String, Boolean>> userExists(
            @Parameter(description = "ID de l'utilisateur à vérifier") @PathVariable Long id) {
        return ResponseEntity.ok(Map.of("exists", userService.userExists(id)));
    }

    // ── GET /users/internal/credentials?email={email} — Credentials pour G10 ──

    @Operation(summary = "Récupérer les identifiants (Interne)", description = "Endpoint réservé à l'API Gateway (G10) pour récupérer le hash du mot de passe lors de l'authentification.")
    @GetMapping("/internal/credentials")
    public ResponseEntity<CredentialsResponseDTO> getCredentials(
            @Parameter(description = "Email de l'utilisateur") @RequestParam String email) {
        return ResponseEntity.ok(userService.getCredentialsByEmail(email));
    }
}
