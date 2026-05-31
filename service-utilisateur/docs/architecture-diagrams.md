# SGITU — Cartographie & Diagrammes de l'Architecture Globale

Ce document présente la vision architecturale et physique du projet **SGITU**, illustrant comment les composants et frameworks (Spring Cloud Gateway, PostgreSQL, Hibernate, Redis, Kafka) s'articulent pour fournir un système robuste, sécurisé et à haute disponibilité.

---

## 1. Diagramme de Composants (Architecture Physique et Logique)

Le diagramme de composants suivant cartographie les différents conteneurs et les technologies utilisées par les groupes, ainsi que les flux de données et d'événements.

```mermaid
graph TB
    subgraph Clients ["Zone Client"]
        Web["Client Web / Mobile"]
    end

    subgraph GatewayZone ["Zone Passerelle & Sécurité"]
        G10["API Gateway (G10) <br/> (Spring Cloud Gateway)"]
        RedisG3[("Cache Révocation (G3-Redis) <br/> (Redis 7)")]
    end

    subgraph ServiceZone ["Microservices Applicatifs (Spring Boot 3.x)"]
        G3["Service Utilisateur (G3) <br/> (Spring Security / JJWT)"]
        G1["Service Billetterie (G1)"]
        G2["Service Abonnement (G2)"]
        G4["Service Coordination (G4)"]
        G5["Service Notification (G5)"]
    end

    subgraph DatabaseZone ["Persistance des Données (JPA / Hibernate)"]
        DB3[("Base Utilisateurs (G3-PostgreSQL) <br/> (PostgreSQL 15)")]
        DB2[("Base Abonnements (G2-MySQL) <br/> (MySQL 8)")]
    end

    subgraph EventZone ["Communication Asynchrone (Événements)"]
        Kafka[("Bus d'Événements <br/> (Kafka CP-7.8)")]
    end

    %% Interactions clients
    Web -->|"HTTPS (Port 8080)"| G10

    %% Routage et Sécurité
    G10 -->|"Vérifie les tokens"| G3
    G3 <-->|"Lecture/Écriture"| RedisG3
    G3 -->|"Hibernate / JPA"| DB3

    %% Routage Gateway vers microservices
    G10 -.->|"HTTP (Port 8081)"| G1
    G10 -.->|"HTTP (Port 8082)"| G2
    G10 -.->|"HTTP (Port 8084)"| G4

    %% Communications internes & Événements
    G2 -->|"Hibernate / JPA"| DB2
    G3 -->|"Publie WELCOME"| Kafka
    Kafka -->|"Consomme WELCOME"| G5
```

### Justification des choix technologiques et frameworks :
1. **Hibernate/JPA** : Utilisé pour la persistance des données relationnelles de G3 (PostgreSQL) et G2 (MySQL). Il garantit un couplage faible et évite d'écrire des requêtes SQL manuelles complexes en fournissant des abstractions de dépôt robustes (`JpaRepository`).
2. **Redis** : Choisi pour le stockage à haute performance (mémoire vive) de la liste noire des tokens révoqués lors de la déconnexion, offrant un temps d'accès sub-milliseconde.
3. **Kafka** : Assure le découplage asynchrone des services. Par exemple, lors de la création d'un utilisateur, G3 publie immédiatement un événement `WELCOME` sur Kafka. G5 (le service de notifications) le consomme à son propre rythme pour envoyer le mail de bienvenue, évitant ainsi d'impacter les temps de réponse de l'utilisateur.

---

## 2. Diagramme de Séquence UML (Authentification, Autorisation & Routage)

Ce diagramme présente le flux détaillé d'une requête envoyée par un utilisateur pour accéder à un service métier protégé (ex: la Billetterie de G1) après s'être connecté.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client (Mobile/Web)
    participant G10 as API Gateway (G10)
    participant G3 as Service Utilisateur (G3)
    participant Redis as Cache Redis (G3-Redis)
    participant G1 as Service Billetterie (G1)

    %% Étape 1 : Connexion
    Note over Client, G3: 1. Connexion & Génération du JWT
    Client->>G10: POST /api/auth/login (email, password)
    G10->>G3: POST /auth/login (route interne)
    Note over G3: Validation des identifiants<br/>via Hibernate
    G3-->>G10: Retourne JWT (signé avec clé secrète G3)
    G10-->>Client: Réponse Login + Access Token (JWT)

    %% Étape 2 : Requête métier protégée
    Note over Client, G1: 2. Requête vers un Service Protégé (G1)
    Client->>G10: GET /api/v1/tickets (Header Authorization: Bearer JWT)
    
    %% Étape 3 : Validation
    Note over G10, Redis: 3. Interception & Vérification Sécurité
    G10->>G3: GET /auth/validate (Vérification signature & expiration)
    G3->>Redis: isTokenRevoked(token) ?
    Redis-->>G3: False (Le token n'est pas révoqué)
    G3-->>G10: Token Valide (Retourne userId, email, roles)

    %% Étape 4 : Routage
    Note over G10, G1: 4. Routage & Injection de contexte
    G10->>G1: GET /v1/tickets (En-têtes X-User-Id, X-User-Roles, etc.)
    Note over G1: Vérification des rôles<br/>(ex: ROLE_PASSENGER)
    G1-->>G10: Liste des tickets (200 OK)
    G10-->>Client: Liste des tickets (200 OK)
```
