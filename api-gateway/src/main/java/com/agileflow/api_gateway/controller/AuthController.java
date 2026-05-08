package com.agileflow.api_gateway.controller;

import com.agileflow.api_gateway.dto.AuthResponse;
import com.agileflow.api_gateway.dto.ForgotPasswordRequest;
import com.agileflow.api_gateway.dto.LoginRequest;
import com.agileflow.api_gateway.dto.MessageResponse;
import com.agileflow.api_gateway.dto.RegisterRequest;
import com.agileflow.api_gateway.dto.ResetPasswordRequest;
import com.agileflow.api_gateway.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints publics et protégés de la Gateway G10 — register, login, logout, refresh, reset password")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(
        summary = "Inscription d'un nouvel utilisateur",
        description = "Crée un compte et envoie un email de vérification via G5 Notifications. Le compte reste inactif jusqu'à vérification."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inscription réussie — email de vérification envoyé"),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé"),
        @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    public ResponseEntity<MessageResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/verify-email")
    @SecurityRequirements
    @Operation(
        summary = "Vérification de l'email",
        description = "Active le compte utilisateur à partir du token reçu par email. Durée de validité : 24h."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email vérifié — compte activé"),
        @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    })
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
        summary = "Connexion et émission du JWT",
        description = "Authentifie l'utilisateur et retourne un access token (1h) + refresh token (7j)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connexion réussie — tokens JWT retournés"),
        @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect"),
        @ApiResponse(responseCode = "403", description = "Compte désactivé ou non vérifié")
    })
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(
        summary = "Demande de réinitialisation du mot de passe",
        description = "Envoie un email de reset password via G5. Durée de validité du lien : 15 minutes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email de réinitialisation envoyé"),
        @ApiResponse(responseCode = "404", description = "Email non trouvé")
    })
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(
        summary = "Réinitialisation du mot de passe",
        description = "Change le mot de passe avec le token reçu par email."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé avec succès"),
        @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    })
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Déconnexion et révocation du refresh token",
        description = "Révoque le refresh token en base de données. Nécessite un access token valide."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
        @ApiResponse(responseCode = "401", description = "Access token manquant ou invalide")
    })
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> body) {
        authService.logout(body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("message", "Deconnexion reussie"));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(
        summary = "Renouvellement de l'access token",
        description = "Génère un nouvel access token à partir d'un refresh token valide et non révoqué."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nouveau token émis"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalide ou révoqué")
    })
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refreshToken(body.get("refreshToken")));
    }
}
