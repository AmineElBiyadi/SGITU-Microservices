# Explication simple — Remarques prof G4

Ce document explique **chaque remarque** comme si on parlait entre nous, sans jargon inutile.

---

## 1. Codes HTTP 409 (véhicule déjà en mission)

**Ce que la prof veut :**  
Si le bus `VH-001` est déjà sur une mission **EN_COURS**, on ne peut pas en créer une deuxième → le serveur répond **409 Conflict** (conflit), pas une erreur vague.

**Chez nous :**  
C’est dans `MissionService` + visible dans Swagger sur `POST /api/g4/missions`.

**Pour toi :**  
Test Postman : créer 2 missions `EN_COURS` avec le même `vehiculeId` → la 2ᵉ doit être **409**.

---

## 2. Exemples JSON (Swagger / Postman)

**Ce que la prof veut :**  
Montrer des **exemples concrets** de ce qu’on envoie dans le body (mission, retard, déviation, notification).

**Chez nous :**  
- Dossier `postman/examples/`  
- Exemples dans Swagger sur missions, events, notifications  

**Pour toi :**  
Ouvre Swagger → tu vois un exemple déjà rempli → copie pour tester.

---

## 3. Retard / déviation sans bloquer la mission

**Ce que la prof veut :**  
Un retard ne doit **pas annuler** la mission. On **signale** le problème, le bus continue sa mission.

**Chez nous :**  
`detect-delay` et `detect-deviation` créent seulement un **événement** ; le statut mission reste **EN_COURS**.

**Pour toi :**  
Après un retard → `GET /api/g4/missions/1` → tu dois encore voir `"statut": "EN_COURS"`.

---

## 4. Résilience (si G5 ou G1 est down)

**Ce que la prof veut :**  
Si un autre service est éteint, G4 ne doit **pas planter** (erreur 500 partout).

**Chez nous :**  
- G5 : si injoignable → réponse `DEGRADED` (pas de crash)  
- Resilience4j (retry, circuit breaker) : **pas encore** — à dire « évolution » au rapport  

**Pour toi :**  
À la soutenance : « Si G5 est down, on dégrade proprement ; Resilience4j est prévu. »

---

## 5. Rôles G3 / G10 / G4

**Ce que la prof veut :**  
Les **mêmes noms de rôles** partout : G3 donne les rôles, G10 met dans le JWT, G4 vérifie.

| Compte démo | Rôle |
|-------------|------|
| gestionnaire.reseau | OPERATOR (réseau) |
| gestionnaire.flotte | DISPATCHER (flotte) |
| admin.technique | ADMIN_G4 |

**Chez nous :** `SecurityConfig.java` + doc `ROLES_G3_G4_ALIGNMENT.md`.

**Pour toi :**  
Login avec `gestionnaire.flotte` pour créer des missions ; `gestionnaire.reseau` pour les lignes.

---

## 6. /health et /logs sans token

**Ce que la prof veut :**  
Pouvoir voir si le service va bien **sans se connecter** (comme une sonde).

**Chez nous :**  
- `GET /api/g4/health` → public  
- `GET /api/g4/logs` → public  

**Pour toi :**  
Dans Postman, appelle `/api/g4/logs` **sans** header `Authorization` → ça doit marcher (200).

---

## 7. Docker + Kafka

**Ce que la prof veut :**  
Dans Docker, les services se parlent avec le **nom** `kafka`, pas `localhost`.

**Chez nous :**  
`docker-compose.yml` : `kafka:9092`, image `apache/kafka:3.7.0`.

**Pour toi :**  
`docker compose up -d` → 3 conteneurs verts (postgres, kafka, g4).

---

## 8. Diagramme Mission ↔ événements

**Ce que la prof veut :**  
Sur le **dessin UML** : une Mission peut avoir **plusieurs** événements (retard, déviation…).

**Chez nous :**  
- **Code** : oui (`mission_id` en base)  
- **Fichier** : `docs/diagrams/mission-coordination-events.puml`  

**Pour toi :**  
Exporter ce fichier en **image PNG** et le mettre dans le **rapport PDF** (pas besoin de changer le code).

---

## 9. Flèche G4 → G3 (valider conducteur)

**Ce que la prof veut :**  
Sur le schéma : G4 demande à G3 si le conducteur existe.

**Chez nous :**  
- Schéma : `docs/diagrams/architecture-externe-g3-g7.puml`  
- Code : `G3UserClient` (optionnel, désactivé par défaut)  

Activer validation :

```properties
sgitu.integration.g3-validation-enabled=true
```

(G3 doit tourner sur le port 8083.)

**Pour toi :**  
Mettre la flèche dans le rapport ; expliquer à l’oral.

---

## 10. Positions G7 (Kafka)

**Ce que la prof veut :**  
G7 envoie la position GPS ; G4 **écoute** et peut détecter une déviation.

**Chez nous :**  
`G7VehiclePositionKafkaConsumer` + topic `vehicule-positions`.

**Pour toi :**  
« G7 publie sur Kafka, G4 consomme et crée un événement DEVIATION si besoin, sans bloquer la mission. »

---

## Ce qui n’est PAS dans le code (normal)

| Élément | Où le faire |
|---------|-------------|
| Rapport PDF final | Word / LaTeX |
| Slides soutenance | PowerPoint |
| Captures Postman | Dossier `captures/` |
| ZIP livraison | Assembler les fichiers |

Le **README.md** à la racine explique comment lancer le projet (exigence livraison).
