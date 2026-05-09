# SGITU - Microservice Gestion des Incidents (G9)

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.x-brightgreen.svg)
![Apache Kafka](https://img.shields.io/badge/Kafka-Event_Driven-black.svg)
![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)

Le microservice **Gestion des Incidents (G9)** est le centre névralgique du projet **SGITU** (Système de Gestion Intelligente des Transports Urbains). Il agit comme la passerelle principale entre le monde physique (capteurs IoT des véhicules, applications usagers) et la salle de contrôle numérique.

## 🎯 Objectif et Fonctionnalités

Ce module gère l'intégralité du cycle de vie des anomalies survenant sur le réseau de transport (pannes mécaniques, accidents, routes encombrées, etc.). 

*   **Réception Omnicanale** : Accepte les signalements via API REST pour les acteurs humains (passagers, conducteurs) et via un Consumer Kafka pour la télémétrie IoT des bus de la ville.
*   **Smart-Regrouping (Anti-doublons)** : Afin d'éviter de saturer les tableaux de bord des superviseurs en cas d'incident majeur, le système intègre un algorithme géographique s'appuyant sur la formule de *Haversine*. Il détecte les requêtes géographiquement et temporellement proches et les regroupe dans l'historique d'un incident parent sans créer de nouveaux tickets.
*   **Machine à États Stricte** : Pilote de manière sécurisée les transitions de l'incident. Le passage entre `NOUVEAU` → `ANALYSE` → `ASSIGNE` → `EN_TRAITEMENT` → `RESOLU` → `CLOTURE` est verrouillé au niveau du backend pour empêcher toute anomalie de gestion (ex: clôturer un ticket non résolu).
*   **Chef d'Orchestre Événementiel** : L'architecture est totalement non-bloquante. G9 déclenche des actions asynchrones vers d'autres services via Apache Kafka : déviation du trafic, alertes utilisateurs, envoi de rapports statistiques.
*   **Audit Trail & SLA** : Chaque action est historisée dans une table dédiée (`Action`) avec l'ID exact de son auteur (extrait dynamiquement du JWT). Le système calcule dynamiquement le temps limite de résolution (SLA) selon la gravité de l'alerte.

## 🏗️ Architecture et Technologies

L'application repose sur les paradigmes de l'Architecture Hexagonale et de l'Event-Driven Architecture (EDA).

*   **Core Backend** : Java 21, Spring Boot 3.2.x
*   **Base de Données** : MySQL 8 (Modélisation via Spring Data JPA / Hibernate)
*   **Message Broker** : Apache Kafka (Topics dédiés pour le transport, l'analytique et les notifications)
*   **Sécurité** : JWT (JSON Web Tokens) validés par la Gateway et traités via des filtres personnalisés (`HeaderAuthenticationFilter`)
*   **Documentation API** : OpenAPI 3 (Swagger UI) intégré nativement
*   **Conteneurisation** : Déploiement via Docker et Docker Compose avec *Multi-Stage Build*.

## 🚀 Démarrage Rapide (Docker)

L'environnement de déploiement `docker-compose.yml` démarre de manière synchrone le service G9, ainsi qu'une base de données MySQL persistante et un cluster Kafka (avec Zookeeper).

```bash
# 1. Cloner le repository
git clone https://github.com/votre-org/SGITU-Gestion-Incidents.git
cd SGITU-Gestion-Incidents/service-gestion-incidents

# 2. Lancer l'environnement complet
docker-compose up --build -d

# 3. Vérifier les logs de démarrage
docker-compose logs -f service-gestion-incidents
```

L'API sera disponible sur : **http://localhost:8089**  
La documentation technique interactive (Swagger) sera accessible sur : **http://localhost:8089/swagger-ui.html**

## 📡 Détail des Endpoints de l'API

L'API REST est protégée par des rôles (`VOYAGEUR`, `CONDUCTEUR`, `SUPERVISEUR_INCIDENTS`). L'API Gateway en amont injecte les headers `X-User-Id` et `X-User-Role`.

### Gestion et Signalement des Incidents
*   `POST /api/incidents/signaler` : Créer un nouveau signalement *(Rôles : VOYAGEUR, CONDUCTEUR, SUPERVISEUR)*.
*   `GET /api/incidents` : Lister et filtrer les incidents *(Rôles : AGENT, SUPERVISEUR)*.
*   `GET /api/incidents/{id}` : Récupérer la vue consolidée d'un incident *(Rôles : VOYAGEUR, CONDUCTEUR, AGENT, SUPERVISEUR)*.
*   `GET /api/incidents/{id}/suivi` : Récupérer l'historique complet et immuable d'un incident (Audit Trail) *(Rôles : AGENT, SUPERVISEUR)*.

### Opérations Métier de la Machine à États (Accès Réservé)
*   `PUT /api/incidents/{id}/statut` : Faire avancer l'incident vers `ANALYSE`, `EN_TRAITEMENT`, `RESOLU` *(Rôles : AGENT, SUPERVISEUR)*.
*   `PUT /api/incidents/{id}/affecter` : Assigner un technicien spécifique à l'intervention *(Rôle : SUPERVISEUR)*.
*   `PUT /api/incidents/{id}/escalader` : Faire passer l'incident en `CRITIQUE` *(Rôle : SUPERVISEUR)*.
*   `PUT /api/incidents/{id}/cloturer` : Fermer définitivement l'incident *(Rôle : SUPERVISEUR)*.
*   `PUT /api/incidents/{id}/annuler` : Invalider une fausse alerte et informer les services tiers *(Rôle : SUPERVISEUR)*.

### Reporting & Tableaux de Bord
*   `GET /api/rapports/generer?periode={periode}` : Obtenir un rapport détaillé des statistiques d'intervention *(Rôle : SUPERVISEUR)*.
*   `GET /api/rapports/dashboard` : Obtenir les KPIs en temps réel pour l'affichage en salle de contrôle *(Rôle : SUPERVISEUR)*.

## ✉️ Intégration Kafka (Événements)

Le microservice interagit de manière totalement asynchrone avec l'écosystème SGITU afin d'éviter les goulots d'étranglement :

*   **🎧 Consumer `suivi-vehicules`** : Écoute en continu les anomalies remontées par les véhicules connectés (G7). Un incident est généré automatiquement avec la source `IOT`.
*   **📢 Producer `incidents-transport`** : G9 ordonne au service de Gestion du Trafic (G4) de dévier les itinéraires (Événement `CONFIRME`) ou de reprendre une circulation normale (Événement `RESOLU`).
*   **📢 Producer `notifications`** : G9 demande au service de Communication (G5) d'alerter les usagers par Push, les techniciens par SMS, ou la direction par Email.
*   **📢 Producer `analytique`** : G9 envoie le dossier technique complet (statistiques, heures de traitement, preuves) au service de Data Warehouse (G8) lors de la clôture de l'incident.

## 🛡️ Modèle de Sécurité (Stateless)

Ce microservice suit le pattern "Stateless Authentication". Il assume qu'une API Gateway a déjà intercepté et vérifié le token JWT du client.
1. La requête arrive avec les headers `X-User-Id` et `X-User-Role`.
2. Le `HeaderAuthenticationFilter` intercepte la requête et hydrate le `SecurityContextHolder` de Spring Security.
3. Les méthodes du Controller sont sécurisées via les annotations `@PreAuthorize("hasRole('SUPERVISEUR_INCIDENTS')")`, garantissant un contrôle d'accès fin aux fonctions d'escalade, de clôture et d'annulation.
4. Le `X-User-Id` est réutilisé en profondeur dans la couche métier pour alimenter le champ `auteurId` de la table d'historisation `Action`.

---
*Projet développé dans le cadre du cursus SGITU.*
