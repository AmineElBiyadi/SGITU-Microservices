# Contrat d'intégration G4 ↔ G7 — Version finale (proposée par G4)

**Projet :** SGITU — Smart Global Intelligent Transport Urbain  
**Microservices :** G4 Coordination des transports · G7 Suivi / référentiel flotte  
**Version :** 1.0 — alignée sur le document G7 « Contrat d'Intégration G4 ↔ G7 — Version finale validée » et les réponses échangées  
**Date :** 20 mai 2026  
**Ports :** G4 `8084` · G7 `8087` · Kafka `9092` (host) / `kafka:9092` (Docker)

---

## 1. Objet

Ce document fixe les **rôles**, le **flow métier**, les **topics Kafka**, les **payloads JSON** et les **appels REST** entre G4 et G7. Il vise un alignement **sans ambiguïté** pour la promo, les tests Postman et la soutenance.

**Accusé de réception :** l'équipe G4 valide le contrat transmis par G7 et l'enrichit des précisions ci-dessous (champs Kafka, `createdAt`, séparation ligne/conducteur).

---

## 2. Rôles et responsabilités

| Service | Responsabilités |
|---------|-----------------|
| **G7** | Crée les véhicules (`DISPONIBLE`) · stocke le statut opérationnel · expose REST · **publie** Kafka `vehicle.registered` et `vehicule-positions` |
| **G4** | Référentiel local des véhicules connus · **affecte** véhicule ↔ ligne · **assigne** le conducteur · notifie G7 (`EN_SERVICE`) · crée les missions · consomme les positions GPS |

**Règle métier validée (G7, Q2) :** la **ligne transport** n'est **pas** définie par G7 à la création du véhicule. Le champ `ligne` dans `vehicle.registered` reste **vide côté G7** jusqu'à décision G4. **L'affectation ligne est le rôle du groupe 4.**

**Conducteur :** à la création, G7 peut publier `conducteurId: null`. L'assignation effective du conducteur est faite par **G4** (`chauffeurId` dans `POST /api/g4/affectations`, identifiant aligné G3).

---

## 3. Accords communs (réponses G7 aux questions G4)

| # | Question G4 | Réponse G7 | Conséquence |
|---|-------------|------------|-------------|
| 1 | Publication `vehicle.registered` à chaque création ? | **Oui** | G4 consomme systématiquement ; secours `POST /api/g4/vehicules/sync-from-g7/{id}` si Kafka en échec |
| 2 | Champ `ligne` G7 vide jusqu'à affectation G4 ? | **Oui** — affectation ligne = **G4** | `ligne` Kafka = info G7 (souvent vide) ; ligne réseau G4 = `ligneId` (REST affectation) |
| 3 | Fin d'affectation : qui remet `DISPONIBLE` chez G7 ? | **G7 ne gère pas ce cas aujourd'hui** | **Point ouvert** — voir § 8 |
| 4 | `vehiculeId` = UUID G7 ? | **Oui** | G4 rejette tout identifiant non-UUID (`400 Bad Request`) |

---

## 4. Flow validé (bout en bout)

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | G7 | `POST /api/suivi-vehicules/vehicules` → statut **`DISPONIBLE`** (`ligne` vide) |
| 2 | G7 | Publie Kafka **`vehicle.registered`** (voir § 5.1) |
| 3 | G4 | Consomme et enregistre dans `vehicules_referentiel` (ou secours sync REST § 6.2) |
| 4 | G4 | `POST /api/g4/affectations` — `vehiculeId` (UUID), `ligneId`, `chauffeurId` (optionnel), `statut: ACTIF` |
| 5 | G4 | `PUT /api/suivi-vehicules/vehicules/{vehiculeId}/statut?statut=EN_SERVICE` |
| 6 | G4 | `POST /api/g4/missions` — **même** `vehiculeId` (UUID), mission sur la ligne affectée |
| 7 | G7 | Pendant la mission : publie **`vehicule-positions`** (voir § 5.2) → G4 peut créer un événement `DEVIATION` sans bloquer la mission |

```text
G7 création ──► Kafka vehicle.registered ──► G4 référentiel
                                              │
G4 affectation (ligneId + chauffeurId) ◄──────┘
       │
       ├──► PUT G7 EN_SERVICE
       └──► POST G4 mission
G7 positions ──► Kafka vehicule-positions ──► G4 événements coordination
```

---

## 5. Kafka

### 5.1 Configuration

| Paramètre | Valeur |
|-----------|--------|
| Bootstrap (local) | `localhost:9092` |
| Bootstrap (Docker, réseau `sgitu-network`) | `kafka:9092` |
| Topic enregistrement | `vehicle.registered` |
| Topic positions | `vehicule-positions` |
| Producteur | **G7 uniquement** |
| Consommateur | **G4** (`G7VehicleRegisteredKafkaConsumer`, `G7VehiclePositionKafkaConsumer`) |
| Variables G4 | `SGITU_G7_VEHICLE_REGISTERED_TOPIC`, `SGITU_G7_POSITIONS_TOPIC`, `SGITU_KAFKA_ENABLED=true` |

**Accord Q1 :** un événement **`vehicle.registered` est publié à chaque création** de véhicule chez G7.

### 5.2 Payload `vehicle.registered` (G7 → G4)

**Champs publiés par G7** (schéma producteur) :

| Champ | Type | Obligatoire | Rôle |
|-------|------|:-----------:|------|
| `vehiculeId` | UUID (string) | **Oui** | Clé unique partagée G4/G7 |
| `immatriculation` | string | Non | Métadonnée flotte |
| `type` | string | Non | ex. `BUS` |
| `ligne` | string | Non | **Souvent vide** à la création (G7) ; **ne remplace pas** `ligneId` G4 |
| `statut` | string | Non | Attendu : `DISPONIBLE` à la création |
| `conducteurId` | string / null | Non | **Souvent `null`** ; assignation réelle = G4 (`chauffeurId` affectation) |
| `createdAt` | ISO-8601 | Non | Date de création côté G7 (**nom officiel** ; pas `timestamp`) |

**Exemple (création typique) :**

```json
{
  "vehiculeId": "53c31262-591a-44d4-8872-51e84611ac5e",
  "immatriculation": "BUS-G4-001",
  "type": "BUS",
  "ligne": null,
  "statut": "DISPONIBLE",
  "conducteurId": null,
  "createdAt": "2026-05-20T10:00:00Z"
}
```

**Côté G4 (consommation) :** champs **requis** pour traiter le message : `vehiculeId` uniquement. Les autres champs sont **acceptés** et mappés si présents. G4 aligne le désérialisation sur **`createdAt`** (compatibilité ascendante : alias `timestamp` accepté le temps de la transition).

### 5.3 Payload `vehicule-positions` (G7 → G4)

| Champ | Type | Obligatoire | Rôle |
|-------|------|:-----------:|------|
| `vehiculeId` | UUID (string) | **Oui** | Véhicule suivi |
| `lat` | number | **Oui** | Latitude |
| `long` | number | **Oui** | Longitude (JSON `long`) |
| `ligneId` | string | Non | Contexte GPS / ligne signalée par G7 (ex. `"L12"`) |
| `vitesse` | number | Non | km/h |
| `timestamp` | ISO-8601 | Non | Horodatage de la position |

**Exemple :**

```json
{
  "vehiculeId": "53c31262-591a-44d4-8872-51e84611ac5e",
  "ligneId": "L12",
  "lat": 35.578,
  "long": -5.368,
  "vitesse": 42.5,
  "timestamp": "2026-05-20T14:30:00Z"
}
```

**Comportement G4 :** validation du contrat → log supervision `KAFKA-G7` → si mission `EN_COURS` et écart de ligne détecté → création événement **`DEVIATION`** ; la mission **reste** `EN_COURS`.

---

## 6. REST

### 6.1 G7 expose (G4 consomme)

| Méthode | Endpoint | Codes | Usage G4 |
|---------|----------|-------|----------|
| `GET` | `/api/suivi-vehicules/vehicules/{vehiculeId}` | `200` / `404` | Sync secours, lecture statut |
| `PUT` | `/api/suivi-vehicules/vehicules/{vehiculeId}/statut?statut={valeur}` | `200` | Après affectation G4 : **`EN_SERVICE`** |

**Statuts `StatutVehicule` supportés par G7 (liste commune) :**

`DISPONIBLE` · `EN_SERVICE` · `EN_PAUSE` · `ARRET_PROLONGE` · `INCIDENT` · `EN_PANNE` · `HORS_SERVICE`

**Base URL G4 (config) :** `SGITU_G7_URL` → ex. `http://g7-service:8087` (Docker) ou `http://localhost:8087` (local).

### 6.2 G4 expose (G7 / gateway / tests)

| Méthode | Endpoint | Auth | Usage |
|---------|----------|------|--------|
| `GET` | `/api/g4/vehicules/disponibles` | JWT | Liste véhicules prêts pour affectation |
| `GET` | `/api/g4/vehicules/{vehiculeId}` | JWT | Détail référentiel G4 |
| `POST` | `/api/g4/vehicules/sync-from-g7/{vehiculeId}` | JWT DISPATCHER / G4_ADMIN | **Secours** si message Kafka perdu |
| `POST` | `/api/g4/affectations` | JWT DISPATCHER | **Affectation ligne + conducteur** (décision G4) |
| `POST` | `/api/g4/missions` | JWT DISPATCHER | Mission ; `409` si même véhicule déjà `EN_COURS` |

**Exemple affectation G4 (ligne + conducteur — c'est ici que G4 « connecte » conducteur et ligne) :**

```json
POST /api/g4/affectations
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "vehiculeId": "53c31262-591a-44d4-8872-51e84611ac5e",
  "ligneId": 1,
  "chauffeurId": "42",
  "dateDebut": "2026-05-20T08:00:00Z",
  "statut": "ACTIF"
}
```

→ G4 appelle automatiquement `PUT .../statut?statut=EN_SERVICE` chez G7.

**Exemple mission G4 :**

```json
POST /api/g4/missions
{
  "vehiculeId": "53c31262-591a-44d4-8872-51e84611ac5e",
  "chauffeurId": "42",
  "ligneId": 1,
  "trajetId": 1,
  "statut": "EN_COURS",
  "plannedStart": "2026-05-20T08:00:00Z"
}
```

### 6.3 Identifiant véhicule

- **`vehiculeId`** = UUID fourni par G7, **identique** dans Kafka, REST G7, affectation G4 et mission G4.
- G4 **n'invente pas** de codes internes type `VH-001`.
- Path ou body avec identifiant non-UUID → **`400 Bad Request`**.

---

## 7. Infrastructure commune

| Service | Hostname Docker | Port |
|---------|-----------------|------|
| G7 | `g7-service` | 8087 |
| G4 | `g4-coordination` | 8084 |
| Kafka | `kafka` | 9092 |
| Réseau | `sgitu-network` | — |

Fichiers de référence G4 : `docker-compose.yml`, `.env.example`, `docs/CONTRATS_ALIGNES_G4.md`.

---

## 8. Point ouvert — fin d'affectation / retour `DISPONIBLE`

**Réponse G7 (Q3) :** à la fin d'affectation côté G4, **G7 ne gère pas encore** le retour automatique en `DISPONIBLE`.

**Proposition G4 (à valider par G7) :**

- Lors de la clôture ou désactivation d'une affectation (`statut` ≠ `ACTIF` ou `dateFin` renseignée), **G4** enverra :
  - `PUT /api/suivi-vehicules/vehicules/{vehiculeId}/statut?statut=DISPONIBLE`
  - mise à jour du référentiel G4 (`disponiblePourAffectation = true`)
- Si l'API G7 refuse ce `PUT`, les équipes documenteront une procédure manuelle ou une évolution G7.

**Statut :** en attente de confirmation G7.

---

## 9. Prérequis d'intégration (checklist promo)

- [ ] Kafka accessible depuis G4 et G7 (`kafka:9092` en Docker)
- [ ] Topics identiques : `vehicle.registered`, `vehicule-positions`
- [ ] G7 crée un véhicule → G4 voit le véhicule (`GET /api/g4/vehicules/disponibles` ou logs `KAFKA-G7-REGISTER`)
- [ ] G4 affectation `ACTIF` → G7 passe `EN_SERVICE`
- [ ] G4 mission avec le même UUID → `201` (ou `409` testé volontairement)
- [ ] Optionnel : message `vehicule-positions` → événement `DEVIATION` sans changement statut mission

---

## 10. Synthèse des règles (encadré vert — alignement commun)

| Règle | Responsable |
|-------|-------------|
| G7 crée le véhicule `DISPONIBLE` | G7 |
| G7 publie `vehicle.registered` à chaque création | G7 |
| `ligne` Kafka souvent vide ; affectation ligne réseau | **G4** (`ligneId`) |
| `conducteurId` Kafka souvent `null` ; conducteur mission/affectation | **G4** (`chauffeurId`, id G3) |
| G4 ne crée pas de clé non-UUID | G4 |
| G4 notifie `EN_SERVICE` après affectation `ACTIF` | G4 → G7 REST |
| Positions GPS et déviation | G7 Kafka → G4 |
| Retour `DISPONIBLE` fin d'affectation | **À valider** (§ 8) |

---

## 11. Validation des équipes

| Équipe | Nom / contact | Date | Signature / OK |
|--------|---------------|------|----------------|
| G4 — Coordination transport | __________________ | ___/___/2026 | ☐ |
| G7 — Suivi véhicules | __________________ | ___/___/2026 | ☐ |

---

*Document rédigé par l'équipe G4 — à transmettre à G7 pour accord mutuel. Référence technique interne : `service-coordination-transport/docs/CONTRATS_ALIGNES_G4.md`.*
