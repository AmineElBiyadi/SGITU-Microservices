# SGITU — Structure du Payload Généralisé & Maintenance

Ce document définit les normes d'interopérabilité et la structure des payloads généralisés échangés au sein de l'écosystème **SGITU** (Système de Gestion Intégrée des Transports Urbains). Il sert de guide de maintenance technique pour les développeurs des groupes **G1 (Billetterie)**, **G2 (Abonnement)**, **G3 (Utilisateur)**, et **G4 (Coordination des Transports)**.

---

## 1. Objectifs & Cinématique Globale

Dans une architecture microservices, l'**API Gateway (G10)** agit comme l'unique point d'entrée. Elle valide le jeton JWT signé par le **Service Utilisateur (G3)**, extrait l'identité de l'utilisateur, puis propage cette information sous forme d'**en-têtes HTTP standardisés** vers les services métiers.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client (Mobile/Web)
    participant G10 as API Gateway (G10)
    participant G3 as Service Utilisateur (G3)
    participant Service as Service Métier (G1/G2/G4)

    Client->>G10: Requête HTTP avec Authorization: Bearer <JWT>
    G10->>G3: GET /auth/validate (Optionnel si validation locale de signature)
    G3-->>G10: Statut de validité du jeton
    Note over G10: Extraction des claims du JWT:<br/>userId, email, roles
    G10->>Service: Requête HTTP enrichie avec les en-têtes X-User-*
    Note over Service: Extraction de l'identité<br/>depuis les en-têtes HTTP
    Service-->>G10: Réponse (Payload unifié)
    G10-->>Client: Réponse finale
```

---

## 2. En-têtes HTTP de Propagation de Contexte

Afin de garantir la traçabilité et la sécurité sans forcer chaque microservice à valider à nouveau le JWT, la Gateway injecte systématiquement les en-têtes suivants dans toutes les requêtes dirigées vers les microservices internes :

| En-tête HTTP | Type | Description | Exemple |
| :--- | :--- | :--- | :--- |
| `X-User-Id` | Long | Identifiant unique de l'utilisateur dans la base G3 | `42` |
| `X-User-Email` | String | Email de l'utilisateur authentifié | `jean.dupont@etu.univ.fr` |
| `X-User-Roles` | String | Rôles de l'utilisateur (séparés par des virgules) | `ROLE_PASSENGER,ROLE_STUDENT` |
| `X-Correlation-Id` | UUID | Identifiant unique de requête pour le traçage distribué | `f81d4fae-7dec-11d0-a765-00a0c91e6bf6` |

---

## 3. Structure Unifiée des Réponses d'Erreur

Tous les microservices (G1, G2, G3, G4) doivent adhérer à la même structure de payload en cas d'erreur. Cela permet à l'API Gateway (G10) et aux clients de traiter les anomalies de manière uniforme.

### Format JSON d'Erreur (Conforme à RFC 7807)

```json
{
  "timestamp": "2026-05-23T18:10:00.000000",
  "status": 404,
  "error": "Not Found",
  "message": "Ressource introuvable ou accès non autorisé.",
  "path": "/api/users/99"
}
```

### Modèle Java (DTO d'Erreur Standardisé)

```java
package com.sgitu.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Format standardisé des erreurs retournées par l'écosystème SGITU")
public class ErrorResponseDTO {

    @Schema(description = "Horodatage ISO-8601 de l'erreur", example = "2026-05-23T18:10:00.000000")
    private String timestamp;

    @Schema(description = "Code de statut HTTP", example = "404")
    private int status;

    @Schema(description = "Libellé de l'erreur HTTP", example = "Not Found")
    private String error;

    @Schema(description = "Message d'erreur descriptif et compréhensible", example = "Utilisateur introuvable avec l'id : 99")
    private String message;

    @Schema(description = "URI de la requête d'origine ayant échoué", example = "/api/users/99")
    private String path;
}
```

---

## 4. Guide d'Implémentation & Exemples de Code

### A. Extraction des En-têtes dans les Services Métiers (G1, G2, G4)

Pour récupérer l'identité de l'utilisateur dans vos contrôleurs Spring Boot, utilisez l'annotation `@RequestHeader` :

```java
@RestController
@RequestMapping("/tickets")
public class TicketController {

    @PostMapping("/buy")
    public ResponseEntity<TicketResponse> buyTicket(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Roles") String rolesStr,
            @RequestBody TicketPurchaseRequest request) {

        System.out.println("Achat de ticket initié par l'utilisateur ID: " + userId);
        // Traitement métier de la billetterie (G1)...

        return ResponseEntity.ok(new TicketResponse("SUCCESS", userId));
    }
}
```

### B. Intercepteur pour le Traçage Distribué (Correlation ID)

Pour propager le `X-Correlation-Id` lors des appels HTTP inter-services (via `RestTemplate` ou `WebClient`), utilisez un intercepteur :

```java
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.slf4j.MDC;
import java.io.IOException;

public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            request.getHeaders().add("X-Correlation-Id", correlationId);
        }
        return execution.execute(request, body);
    }
}
```

---

## 5. Maintenance & Évolution

1. **Non-modification des en-têtes existants** : Aucun groupe ne doit modifier ou supprimer les en-têtes préfixés par `X-User-` ou `X-Correlation-` sous peine de briser la sécurité globale.
2. **Ajout d'attributs spécifiques** : Si un groupe a besoin d'informations supplémentaires non incluses dans les en-têtes actuels, une demande formelle doit être adressée au groupe G3 pour enrichir le jeton JWT d'origine et la Gateway G10 pour la propagation.