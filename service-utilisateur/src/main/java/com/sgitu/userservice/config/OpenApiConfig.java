package com.sgitu.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "SGITU — Service Utilisateur (G3)",
        version = "1.0",
        description = """
            API de gestion des utilisateurs et des profils pour la plateforme SGITU.

            **Responsabilités de G3 :**
            - Création et gestion des comptes utilisateurs
            - Gestion des profils (nom, téléphone, adresse, date de naissance)
            - Attribution des rôles (ROLE_PASSENGER, ROLE_DRIVER, ROLE_ADMIN, etc.)
            - Émission des tokens JWT (G3 est l'unique source de vérité pour l'identité)
            - Activation / désactivation des comptes
            - Notification de G8 (Analytics) lors des changements d'état

            ---

            **Gestion des cas d'erreur**

            Toutes les erreurs suivent ce format JSON :
            ```json
            {
              "timestamp": "2026-05-08T10:00:00",
              "status": 404,
              "error": "Not Found",
              "message": "Utilisateur introuvable avec l'id : 99",
              "path": "/api/users/99"
            }
            ```

            | Code HTTP | Cause |
            |-----------|-------|
            | **400** | Données invalides : champ obligatoire manquant, email mal formaté, mot de passe < 8 caractères, rôle inconnu |
            | **401** | Token JWT absent, expiré ou invalide — ou identifiants incorrects sur /auth/login |
            | **403** | Rôle insuffisant (ex : ROLE_ADMIN requis) ou compte désactivé |
            | **404** | Utilisateur introuvable (ID inexistant) |
            | **409** | Email déjà utilisé par un autre compte |
            | **415** | Content-Type incorrect — utiliser `application/json` |
            | **500** | Erreur interne inattendue |

            ---

            **Authentification**

            1. Appeler `POST /auth/login` avec email + mot de passe
            2. Copier le champ `token` de la réponse
            3. Cliquer sur **Authorize 🔒** en haut à droite et coller le token
            4. Toutes les requêtes suivantes incluront automatiquement le header `Authorization: Bearer {token}`

            Endpoints publics (sans token) : `POST /auth/login`, `POST /users`
            """,
        contact = @Contact(name = "Groupe 3 — SGITU", email = "g3@sgitu.ma")
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtenu via POST /auth/login. Coller uniquement le token (sans le préfixe 'Bearer')."
)
public class OpenApiConfig {
}
