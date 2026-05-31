# Configuration JWT et Logging Admin - Service Notification G5

## 1. Acceptation des Tokens JWT de tous les groupes avec Traçabilité

Le service de notification (G5) est configuré pour accepter les tokens JWT provenant de tous les autres groupes (G1-G10) avec un mécanisme de traçabilité vers G10 (Auth Service).

### Architecture de Sécurité avec Traçabilité

#### JWTAuthenticationFilter
- **Fichier**: `src/main/java/ma/sgitu/g5/security/JWTAuthenticationFilter.java`
- **Rôle**: Filtre d'authentification JWT qui accepte les tokens de tous les groupes
- **Approche de traçabilité**:
  1. **Accepte tous les tokens** sans validation stricte de signature
  2. **Extrait les informations de traçabilité**: `userId`, `sourceService`, `roles`
  3. **Génère un Trace ID** unique pour chaque requête
  4. **Envoie les infos à G10** via Kafka pour validation asynchrone
  5. **Traitement spécial pour G3** (Users) avec logging prioritaire
  6. **Compatibilité**: Si aucun token n'est présent, la requête continue sans échouer

#### TracingService
- **Interface**: `src/main/java/ma/sgitu/g5/service/ITracingService.java`
- **Implémentation**: `src/main/java/ma/sgitu/g5/service/TracingServiceImpl.java`
- **Rôle**: Envoie les informations de traçabilité à G10 pour validation
- **Fonctionnement**:
  - Construit un événement de traçabilité avec: traceId, tokenHash, sourceGroup, userId, roles
  - Envoie l'événement au topic Kafka `token-validation`
  - G10 reçoit et valide le token de manière asynchrone
  - G5 ne bloque pas la requête en attendant la validation

#### SecurityConfig
- **Fichier**: `src/main/java/ma/sgitu/g5/config/SecurityConfig.java`
- **Configuration**:
  - Endpoints publics: `/api/notifications/health`, Swagger UI, API docs
  - Endpoints admin: `/api/notifications/admin/**` (nécessite ROLE_ADMIN)
  - Autres endpoints: Acceptent les JWT mais ne les exigent pas (compatibilité)

### Headers Requis pour la Traçabilité

Les groupes doivent envoyer les headers suivants:

```bash
Authorization: Bearer <jwt_token>
X-Source-Group: G3  # ou G1, G2, G4, etc.
X-Trace-Id: <optional_trace_id>  # si non fourni, G5 en génère un
```

### Format du Token JWT attendu

Les tokens JWT doivent contenir les claims suivants:

```json
{
  "sub": "userId",
  "sourceService": "G3_UTILISATEUR",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "exp": 1234567890
}
```

### Configuration

Dans `application.yml`:
```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 heures

spring:
  kafka:
    template:
      default-topic: token-validation  # Topic pour envoyer les infos à G10
```

### Flux de Traçabilité

```
G1/G2/G3/G4/etc → G5 (Notification)
                 ↓ JWTAuthenticationFilter
                 ↓ Extrait infos (userId, sourceGroup, etc.)
                 ↓ Génère Trace ID
                 ↓ TracingService.sendTracingInfo()
                 ↓ Kafka topic: token-validation
                 ↓ G10 (Auth Service) reçoit
                 ↓ G10 valide le token asynchrone
                 ↓ G5 continue sans attendre (non-blocking)
```

### Utilisation par les autres groupes

Chaque groupe (G1-G10) peut envoyer des requêtes au service G5 avec:

```bash
curl -X POST http://localhost:8085/api/notifications/send \
  -H "Authorization: Bearer <jwt_token>" \
  -H "X-Source-Group: G4" \
  -H "Content-Type: application/json" \
  -d '{
    "notificationId": "uuid-g4-001",
    "sourceService": "G4_COORDINATION",
    "eventType": "MISSION_CANCELLED",
    "channel": "EMAIL",
    "priority": "HIGH",
    "recipient": {
      "userId": "user123",
      "email": "user@example.com"
    }
  }'
```

### Traitement Spécial pour G3 (Users)

Le groupe G3 (Utilisateurs) reçoit un traitement spécial:
- Logging prioritaire avec tag `[JWT-TRACE] Traitement spécial pour G3 (Users)`
- Priorité HIGH dans l'événement de traçabilité envoyé à G10
- Surveillance accrue pour les opérations sensibles (authentification, changement de mot de passe)

## 2. Logging pour l'Administrateur

### Configuration de Logging

#### Fichier de Configuration
- **Fichier**: `src/main/resources/logback-admin.xml`
- **Emplacement des logs**: `logs/admin-operations.log`
- **Rotation**: Quotidienne, conservation 30 jours, taille max 100MB

#### Loggers Configurés

1. **ma.sgitu.g5.admin** - Opérations d'administration
2. **ma.sgitu.g5.security** - Opérations de sécurité et traçabilité JWT
3. **ma.sgitu.g5.service.TracingService** - Opérations de traçabilité (envoi à G10)
4. **ma.sgitu.g5.controller.NotificationController** - Opérations de notification
5. **ma.sgitu.g5.exceptions** - Erreurs et exceptions
6. **ma.sgitu.g5.controller.KafkaConsumerController** - Événements Kafka

#### Configuration application.yml

```yaml
logging:
  level:
    ma.sgitu.g5.notifications: DEBUG
    ma.sgitu.g5.admin: INFO
    ma.sgitu.g5.security: INFO
    ma.sgitu.g5.service.TracingService: INFO
    org.springframework.kafka: INFO
    org.springframework.mail: DEBUG
  file:
    name: logs/notification-service.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
```

### Endpoints d'Administration

#### AdminController
- **Fichier**: `src/main/java/ma/sgitu/g5/controller/AdminController.java`
- **Base Path**: `/api/notifications/admin`
- **Sécurité**: Nécessite ROLE_ADMIN

#### Endpoints Disponibles

1. **GET /api/notifications/admin/stats**
   - Statistiques globales (par statut, par canal)
   - Log: `[ADMIN] Récupération des statistiques globales`

2. **GET /api/notifications/admin/notifications**
   - Liste toutes les notifications avec filtres
   - Filtres: status, channel, sourceService, startDate, endDate
   - Log: `[ADMIN] Liste des notifications - filters: ...`

3. **GET /api/notifications/admin/notifications/failed**
   - Liste uniquement les notifications en échec
   - Log: `[ADMIN] Récupération des notifications en échec`

4. **GET /api/notifications/admin/notifications/by-source/{sourceService}**
   - Liste les notifications par service source (G1-G10)
   - Log: `[ADMIN] Récupération des notifications pour le service source: ...`

5. **DELETE /api/notifications/admin/notifications/{id}**
   - Supprime définitivement une notification
   - Log: `[ADMIN] Suppression de la notification ID: ...`

6. **POST /api/notifications/admin/notifications/{id}/force-retry**
   - Force la relance d'une notification quel que soit son statut
   - Log: `[ADMIN] Force retry pour la notification ID: ...`

7. **GET /api/notifications/admin/health/detailed**
   - Health check détaillé avec métriques
   - Log: `[ADMIN] Health check détaillé demandé`

### Exemple d'utilisation des endpoints admin

```bash
# Récupérer les statistiques
curl -X GET http://localhost:8085/api/notifications/admin/stats \
  -H "Authorization: Bearer <admin_jwt_token>"

# Lister les notifications en échec
curl -X GET http://localhost:8085/api/notifications/admin/notifications/failed \
  -H "Authorization: Bearer <admin_jwt_token>"

# Forcer la relance d'une notification
curl -X POST http://localhost:8085/api/notifications/admin/notifications/123/force-retry \
  -H "Authorization: Bearer <admin_jwt_token>"
```

## 3. Repository pour Admin

### Méthodes ajoutées à NotificationRepository

```java
// Comptage par statut
long countByStatus(NotificationStatus status);

// Comptage par canal
long countByChannel(String channel);

// Recherche combinée statut + canal
Page<Notification> findByStatusAndChannel(NotificationStatus status, String channel, Pageable pageable);

// Recherche par statut
Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

// Recherche par canal
Page<Notification> findByChannel(String channel, Pageable pageable);

// Recherche par service source
Page<Notification> findBySourceService(String sourceService, Pageable pageable);
```

## 4. Structure des Logs

### Format des Logs

```
2024-05-23 14:30:45.123 [http-nio-8085-exec-1] INFO  ma.sgitu.g5.admin - [ADMIN] Récupération des statistiques globales
2024-05-23 14:30:45.456 [http-nio-8085-exec-1] INFO  ma.sgitu.g5.security - [JWT-TRACE] Trace ID: abc-123 - Token parsé avec succès - User: user123, Token Source: G3_UTILISATEUR, Header Source: G3
2024-05-23 14:30:45.789 [http-nio-8085-exec-1] INFO  ma.sgitu.g5.service.TracingService - [TRACING] Trace ID: abc-123 - Source: G3 - User: user123 - Token Valid: true - Sending to G10
2024-05-23 14:30:46.789 [http-nio-8085-exec-2] WARN  ma.sgitu.g5.admin - [ADMIN] Suppression de la notification ID: 456
```

### Emplacement des Fichiers de Logs

- **Logs généraux**: `logs/notification-service.log`
- **Logs admin**: `logs/admin-operations.log`
- **Rotation**: Quotidienne avec conservation 30 jours

## 5. Sécurité et Traçabilité

### Rôles Requis

- **Endpoints publics**: Aucun rôle requis
- **Endpoints standards**: JWT accepté mais non requis (compatibilité)
- **Endpoints admin**: ROLE_ADMIN requis

### Validation JWT avec Traçabilité

- **Acceptation sans validation stricte**: G5 accepte tous les tokens sans bloquer
- **Parsing des informations**: Extrait userId, sourceService, roles du token
- **Traçabilité asynchrone**: Envoie les infos à G10 via Kafka pour validation
- **Non-blocking**: G5 continue le traitement sans attendre la validation de G10
- **Trace ID**: Chaque requête reçoit un ID unique pour traçabilité
- **Traitement spécial G3**: Logging prioritaire pour les tokens du groupe utilisateurs

### Événement de Traçabilité envoyé à G10

```json
{
  "traceId": "abc-123-def-456",
  "timestamp": "2024-05-23T14:30:45",
  "targetService": "G5_NOTIFICATION",
  "tokenHash": "a1b2c3d4",
  "tokenValid": true,
  "tokenSourceService": "G3_UTILISATEUR",
  "headerSourceGroup": "G3",
  "userId": "user123",
  "roles": ["ROLE_USER"],
  "specialHandling": "G3_USERS",
  "priority": "HIGH"
}
```

## 6. Intégration avec les autres groupes

### Services qui envoient des notifications

- **G1 - Billetterie**: Events Kafka (ticket.*) + Headers X-Source-Group: G1
- **G2 - Abonnements**: Events Kafka (abonnement.*) + Headers X-Source-Group: G2
- **G3 - Utilisateurs**: Events Kafka (user-events) + Headers X-Source-Group: G3 (traitement spécial)
- **G4 - Coordination**: REST API + Headers X-Source-Group: G4
- **G6 - Paiement**: REST API + Headers X-Source-Group: G6
- **G8 - Analytics**: REST API + Headers X-Source-Group: G8
- **G9 - Incidents**: REST API + Headers X-Source-Group: G9
- **G10 - Auth**: REST API + Headers X-Source-Group: G10

Tous ces services peuvent envoyer des requêtes avec leur JWT token. G5:
1. Accepte le token sans validation stricte
2. Extrait les informations de traçabilité
3. Envoie les infos à G10 via Kafka pour validation asynchrone
4. Continue le traitement sans bloquer

### Exemple de requête avec traçabilité

```bash
curl -X POST http://localhost:8085/api/notifications/send \
  -H "Authorization: Bearer eyJhbGci..." \
  -H "X-Source-Group: G3" \
  -H "X-Trace-Id: trace-123-abc" \
  -H "Content-Type: application/json" \
  -d '{
    "notificationId": "uuid-g3-001",
    "sourceService": "G3_UTILISATEUR",
    "eventType": "PASSWORD_CHANGED",
    "channel": "EMAIL",
    "priority": "HIGH",
    "recipient": {
      "userId": "user123",
      "email": "user@example.com"
    }
  }'
```

G5 va:
1. Parser le token JWT
2. Logger: `[JWT-TRACE] Trace ID: trace-123-abc - Traitement spécial pour G3 (Users)`
3. Envoyer à G10 via Kafka: `{traceId: "trace-123-abc", headerSourceGroup: "G3", specialHandling: "G3_USERS", priority: "HIGH"}`
4. Traiter la notification normalement
