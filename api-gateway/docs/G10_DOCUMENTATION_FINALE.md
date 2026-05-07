# SGITU - Groupe 10 API Gateway & Securite

## 1. Role de G10

G10 est le point d'entree unique du systeme SGITU.

Apres clarification architecturale, G10 ne possede plus de base utilisateurs. Le service **G3 Gestion des utilisateurs** est la source de verite pour les comptes, profils, roles, permissions, mots de passe, verification email, reset password et emission JWT.

G10 assure :

- validation des JWT emis par G3 ;
- controle d'acces par role ;
- routage vers G1-G9 ;
- propagation des headers utilisateur ;
- gestion des erreurs Gateway ;
- logs avec `X-Correlation-Id` ;
- Swagger/OpenAPI Gateway ;
- execution Docker.

## 2. Execution

Depuis le dossier `api-gateway` :

```powershell
docker compose up -d --build
```

URLs :

```text
Gateway : http://localhost:8080
Health  : http://localhost:8080/actuator/health
Swagger : http://localhost:8080/swagger-ui.html
Logs    : logs/api-gateway.log
```

G10 ne lance plus MySQL/phpMyAdmin car il ne stocke plus les comptes utilisateurs.

## 3. Contrat JWT avec G3

G3 doit emettre un access token JWT signe avec le secret partage configure dans G10.

Claims attendus :

```json
{
  "sub": "user@sgitu.ma",
  "role": "ROLE_USER",
  "userId": 10,
  "iat": 1715000000,
  "exp": 1715003600,
  "jti": "uuid"
}
```

G10 valide :

- signature ;
- expiration `exp` ;
- presence du `sub` ;
- presence du `role`.

## 4. Headers transmis aux microservices

G10 ajoute apres validation JWT :

```http
X-User-Id: 10
X-User-Email: user@sgitu.ma
X-Roles: ROLE_USER
X-Correlation-Id: test-123
```

## 5. Routes principales

### G3 Auth et utilisateurs

| Path | Responsable reel |
| --- | --- |
| `/auth/**` | G3 |
| `/admin/users/**` | G3, protege par `ROLE_ADMIN` |
| `/api/users/**` | G3 |
| `/api/profiles/**` | G3 |

### Autres groupes

| Groupe | Prefixes |
| --- | --- |
| G1 | `/api/v1/tickets/**`, `/api/v1/admin/**`, `/api/v1/ticket-types/**` |
| G2 | `/api/abonnements/**`, `/api/plans/**` |
| G4 | `/api/g4/**`, `/api/v1/operator/status` |
| G5 | `/api/notifications/**` |
| G6 | `/api/payments/**`, `/api/refunds/**`, `/api/payment-accounts/**`, `/api/invoices/**` |
| G7 | `/api/suivi-vehicules/**` |
| G8 | `/api/v1/ingestion/**`, `/api/v1/analytics/**`, `/predict/**` |
| G9 | `/api/incidents/**`, `/api/rapports/**` |

## 6. Securite

Regles appliquees par G10 :

- `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/verify-email`, `/auth/forgot-password`, `/auth/reset-password` sont publics et routes vers G3.
- `/admin/**` demande `ROLE_ADMIN`.
- `/api/v1/admin/**` demande `ROLE_ADMIN`.
- `/api/v1/ticket-types/**` demande `ROLE_ADMIN`.
- `/api/v1/analytics/**` et `/predict/**` demandent `ROLE_ADMIN` ou `ROLE_AGENT`.
- `/api/**` demande un JWT valide.

## 7. Tests

Commande :

```powershell
.\mvnw.cmd clean test
```

Les tests couvrent :

- absence de JWT -> 401 ;
- JWT invalide -> 401 ;
- role insuffisant -> 403 ;
- route inconnue -> 404 structuree ;
- conformite des routes G1, G2, G3, G4, G5, G6, G7, G8 ;
- protection admin/analytics.

## 8. Logs

Fichier :

```text
logs/api-gateway.log
```

Exemple :

```text
Gateway request correlationId=test-g1 method=GET path=/api/v1/tickets/1 user=user@sgitu.ma roles=ROLE_USER
Gateway response correlationId=test-g1 status=200 path=/api/v1/tickets/1
```

## 9. Points d'integration a fournir aux autres groupes

G10 fournit :

- URL Gateway ;
- prefixes de routes ;
- format JWT attendu ;
- header `Authorization: Bearer <JWT>` ;
- headers transmis aux services ;
- codes d'erreur Gateway ;
- regles de securite par prefixe ;
- requirement G3 : JWT signe avec claims `sub`, `role`, `userId`, `iat`, `exp`.

## 10. Conclusion

La responsabilite utilisateur est maintenant correctement separee :

- G3 possede les utilisateurs et emet les JWT.
- G10 valide les JWT, applique la securite transverse et route les requetes.

Cette version est plus conforme aux principes microservices : pas de duplication des comptes, pas de base utilisateur dans la Gateway, et une seule source de verite pour l'identite.
