# Documentation finale - Groupe 10 API Gateway & Securite

## Role du groupe 10

G10 fournit le point d'entree unique du systeme SGITU. Le client appelle la Gateway, la Gateway valide le JWT, applique les regles de securite, ajoute les headers utiles, puis route vers le microservice cible.

G10 gere aussi l'authentification locale : inscription, verification email, login, refresh token, logout, forgot password/reset password, compte admin initial et endpoints admin de securite.

## Demarrage

Depuis le dossier `api-gateway` :

```bash
docker compose up -d --build
```

Services exposes :

- API Gateway : `http://localhost:8080`
- Swagger : `http://localhost:8080/swagger-ui.html`
- Health : `http://localhost:8080/actuator/health`
- phpMyAdmin : `http://localhost:8090`
- MySQL G10 : `localhost:3360`

Connexion phpMyAdmin :

```text
Serveur: gateway-db
Utilisateur: root
Mot de passe: root
Base: gateway_db
```

## Compte admin initial

Au demarrage, G10 cree un admin si l'email n'existe pas encore :

```text
email: admin@sgitu.ma
password: Admin123456
role: ROLE_ADMIN
```

Variables configurables :

```text
G10_ADMIN_BOOTSTRAP_ENABLED=true
G10_ADMIN_EMAIL=admin@sgitu.ma
G10_ADMIN_PASSWORD=Admin123456
```

## Endpoints principaux

Publics :

- `POST /auth/register`
- `GET /auth/verify-email?token=...`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`

Protege :

- `POST /auth/logout`

Admin G10, role `ROLE_ADMIN` obligatoire :

- `GET /admin/users`
- `GET /admin/users/{id}`
- `PUT /admin/users/{id}/role`
- `PUT /admin/users/{id}/status`
- `PUT /admin/users/{id}/email-verification`

## Integration avec G5 Notifications

G10 appelle G5 pour envoyer les emails de verification et de reset password.

URL locale :

```text
http://localhost:8085/api/notifications/send
```

URL Docker :

```text
http://notification-service:8085/api/notifications/send
```

Pour les appels client routes par la Gateway, G10 garde le chemin complet :

```text
Client -> G10: /api/notifications/**
G10 -> G5: /api/notifications/**
```

Il n'y a pas de `RewritePath` sur la route G5, car G5 expose directement `/api/notifications/**`.

Dans `api-gateway/docker-compose.yml`, les notifications sont desactivees par defaut pour tester G10 sans lancer G5 :

```text
G10_NOTIFICATIONS_ENABLED=false
G10_EMAIL_LOG_TOKENS=true
```

Payload REST envoye par G10 a G5 pour verification email :

```json
{
  "notificationId": "auth-8f2a9c2e-1234-45aa-90bb-abcdef123456",
  "sourceService": "AUTH",
  "eventType": "VERIFY_EMAIL",
  "channel": "EMAIL",
  "priority": "NORMAL",
  "recipient": {
    "userId": "10",
    "email": "user@sgitu.ma"
  },
  "metadata": {
    "sourceType": "ACCOUNT",
    "sourceId": 10,
    "verificationLink": "http://localhost:8080/auth/verify-email?token=abc"
  }
}
```

Payload REST envoye par G10 a G5 pour reset password :

```json
{
  "notificationId": "auth-8f2a9c2e-1234-45aa-90bb-abcdef123456",
  "sourceService": "AUTH",
  "eventType": "RESET_PASSWORD",
  "channel": "EMAIL",
  "priority": "NORMAL",
  "recipient": {
    "userId": "10",
    "email": "user@sgitu.ma"
  },
  "metadata": {
    "sourceType": "ACCOUNT",
    "sourceId": 10,
    "resetLink": "http://localhost:8080/auth/reset-password?token=xyz"
  }
}
```

Regle G5 respectee : `eventType` est un champ racine du JSON, separe de `metadata`.

Si G5 impose `Authorization: Bearer <JWT>` pour les appels service-to-service, configurer :

```text
G10_NOTIFICATIONS_BEARER_TOKEN=<token_fourni_par_G5>
```

## JWT fourni par G10

Le JWT contient au minimum :

- `sub` : email utilisateur
- `role` : role principal, exemple `ROLE_USER`
- `iat` : date de creation
- `exp` : date d'expiration
- `jti` : identifiant unique du token

Header attendu par la Gateway :

```text
Authorization: Bearer <accessToken>
```

Headers transmis par G10 aux microservices :

```text
X-User-Id: 10
X-User-Email: user@sgitu.ma
X-Roles: ROLE_USER
X-Correlation-Id: 8f2a9c2e-1234-45aa-90bb-abcdef123456
```

## Tests

Lancer les tests automatises :

```bash
cd api-gateway
.\mvnw.cmd test
```

Les tests couvrent :

- creation du compte admin initial
- login admin
- inscription avec verification email
- refus de login avant verification
- refresh token
- logout et revocation du refresh token
- forgot password / reset password
- protection des endpoints admin par role
- erreurs 401 et 404 structurees

## Regles de securite importantes

- `ROLE_ADMIN` ne peut pas etre cree par inscription publique.
- Le refresh token est stocke en base pour pouvoir etre revoque.
- Les tokens sont revoques lors du logout, du reset password et de certaines actions admin.
- Les routes `/api/**` demandent un JWT valide.
- Les routes `/admin/**` demandent un JWT avec `ROLE_ADMIN`.
