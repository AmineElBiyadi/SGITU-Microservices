# Validation & Tests — Service G5 (Notifications)

Ce document explique comment exécuter les tests, générer les **preuves** (rapports Maven + Postman) pour la section du rapport :

> **Validation & Tests** : Preuves de tests unitaires et d'intégration (Postman collections).

---

## 1. Prérequis

| Outil | Version minimale |
|--------|------------------|
| Java | 21 |
| Maven | 3.9+ |
| Docker Desktop | (pour tests manuels / Postman avec stack complète) |
| Postman | Desktop ou CLI [Newman](https://www.npmjs.com/package/newman) (optionnel, pour rapport HTML Postman) |

Variables d'environnement : fichier `.env` à la racine de `service-notification` (voir `docker-compose.yml`).

---

## 2. Tests automatisés Maven (unitaires + intégration)

### 2.1 Tous les tests

```powershell
cd c:\Users\HP\SGITU-Microservices\service-notification
mvn clean test
```

**Résultat attendu :** `BUILD SUCCESS` et `Tests run: 45, Failures: 0, Errors: 0`.

### 2.2 Tests unitaires uniquement

```powershell
mvn test -Dtest="NotificationServiceImplTest,ChannelRouterImplTest,TemplateServiceImplTest,RetryServiceImplTest,KafkaConsumerControllerTest"
```

| Classe | Type | Couverture |
|--------|------|------------|
| `NotificationServiceImplTest` | Unitaire | Service métier (send, retry, échecs) |
| `ChannelRouterImplTest` | Unitaire | Routage EMAIL/SMS/PUSH/LOG |
| `TemplateServiceImplTest` | Unitaire | Templates par `eventType` |
| `RetryServiceImplTest` | Unitaire | Politique de retry |
| `KafkaConsumerControllerTest` | Unitaire | Consommateurs Kafka G1–G9 (mock) |

### 2.3 Tests d'intégration uniquement

```powershell
mvn test -Dtest="G5ApplicationTests,NotificationControllerIntegrationTest,MicroserviceContractIntegrationTest,KafkaMicroserviceIntegrationTest"
```

| Classe | Type | Couverture |
|--------|------|------------|
| `G5ApplicationTests` | Intégration | Contexte Spring, JPA, API liste |
| `NotificationControllerIntegrationTest` | Intégration | REST + sécurité headers Gateway |
| `MicroserviceContractIntegrationTest` | Intégration | Contrats REST G4, G6, G8, G9, G10 |
| `KafkaMicroserviceIntegrationTest` | Intégration | Kafka embarqué (G1, G3, G6) |

Profil : `application-test.yml` (base H2 en mémoire, pas besoin de MySQL pour les tests Maven).

---

## 3. Rapports de tests (preuves pour le rapport)

### 3.1 Script automatique (recommandé)

```powershell
cd c:\Users\HP\SGITU-Microservices\service-notification
.\scripts\generate-test-reports.ps1
```

Génère :

| Fichier | Description |
|---------|-------------|
| `target/surefire-reports/` | Rapports XML par classe (JUnit) |
| `target/site/surefire-report.html` | **Rapport HTML** lisible (à joindre ou capturer en PDF) |
| `docs/reports/TEST_SUMMARY_YYYYMMDD.txt` | Résumé texte (nombre de tests, succès/échecs) |

### 3.2 Commandes manuelles

```powershell
mvn clean test
mvn surefire-report:report only
```

Ouvrir dans le navigateur :

`file:///.../service-notification/target/site/surefire-report.html`

### 3.3 Ce qu’il faut mettre dans le rapport académique

1. **Capture ou PDF** de `surefire-report.html` (tableau vert = tous les tests passés).
2. **Tableau** listant tests unitaires vs intégration (section 2.2 / 2.3).
3. **Extrait** d’un fichier XML : `target/surefire-reports/TEST-*.xml` (balises `tests`, `failures`, `errors`).
4. **Commande** exécutée : `mvn clean test` + date + résultat `BUILD SUCCESS`.

Exemple de phrase :

> Les 45 tests automatisés (22 unitaires, 23 intégration) ont été exécutés avec Maven Surefire ; le rapport HTML joint confirme 0 échec et 0 erreur.

---

## 4. Tests Postman (API + inter-microservices)

### 4.1 Fichiers à importer

| Fichier | Rôle |
|---------|------|
| `docs/postman/G5-Notification-Full.postman_collection.json` | Collection complète |
| `docs/postman/G5-Notification-Local.postman_environment.json` | Variables `g5BaseUrl`, `gatewayUrl` |

Collection de base (historique) : `docs/postman/G5-Notification.postman_collection.json`.

### 4.2 Démarrer l’environnement

```powershell
cd c:\Users\HP\SGITU-Microservices\service-notification
docker compose up -d
# Attendre MySQL + Kafka healthy, puis :
mvn spring-boot:run
# ou lancer le JAR / conteneur notification-service sur le port 8085
```

Vérifier : `GET http://localhost:8085/api/notifications/health` → `"status":"UP"`.

### 4.3 Exécution manuelle (Postman Desktop)

1. **Import** → Collection + Environment → activer **SGITU G5 — Local**.
2. Dossier **0. Santé & Observabilité** → lancer *Health Check G5*.
3. Dossier **1. Envoi REST direct G5** → EMAIL, SMS, PUSH, LOG.
4. Dossier **5. Simulations inter-microservices** → une requête par groupe (G1…G10).
5. Dossier **4. Via API Gateway G10** (si G10 tourne sur `8080`) → Login puis Send via Gateway.

**Preuve pour le rapport :**

- Capture d’écran du **Collection Runner** avec toutes les requêtes en vert.
- Ou **Export** : Runner → Export Results → JSON/HTML.

### 4.4 Exécution automatisée (Newman + rapport HTML)

Prérequis : Node.js installé.

```powershell
npm install -g newman newman-reporter-htmlextra
cd c:\Users\HP\SGITU-Microservices\service-notification

newman run docs/postman/G5-Notification-Full.postman_collection.json `
  -e docs/postman/G5-Notification-Local.postman_environment.json `
  --folder "0. Santé & Observabilité" `
  --folder "1. Envoi REST direct G5" `
  -r cli,htmlextra `
  --reporter-htmlextra-export docs/reports/postman-report.html
```

Joindre au rapport : `docs/reports/postman-report.html`.

### 4.5 Kafka (preuve intégration événementielle)

Les requêtes du dossier **6. Kafka — Exemples payloads** documentent les messages. Pour une démo live :

1. Ouvrir Kafka UI : `http://localhost:8091`
2. Publier un message sur `user-events` ou `ticket.created` (JSON du dossier 6).
3. Vérifier dans Postman : `GET /api/notifications?sourceService=G3_UTILISATEUR` (avec headers `X-User-Id` / `X-Roles`).

Capture Kafka UI + réponse API = preuve d’intégration G3 → G5.

---

## 5. Modèle de section « Validation & Tests » (rapport prof)

```markdown
### Validation & Tests

#### Tests unitaires
- Outil : JUnit 5 + Mockito
- Commande : `mvn test -Dtest=NotificationServiceImplTest,...`
- Rapport : `target/site/surefire-report.html` (cf. annexe X)
- Résultat : N tests, 0 failure, 0 error

#### Tests d'intégration
- Outil : Spring Boot Test, MockMvc, Embedded Kafka, H2
- Commande : `mvn test -Dtest=*IntegrationTest,G5ApplicationTests`
- Scénarios : API REST, contrats G4/G6/G8/G9/G10, consommation Kafka G1/G3/G6

#### Tests API (Postman)
- Collection : `docs/postman/G5-Notification-Full.postman_collection.json`
- Environnement : `G5-Notification-Local.postman_environment.json`
- Preuve : capture Runner / rapport Newman `postman-report.html`

#### Synthèse
| Type | Nombre | Statut |
|------|--------|--------|
| Unitaires | 22 | OK |
| Intégration | 23 | OK |
| Postman (scénarios manuels/auto) | 30+ requêtes | OK |
```

---

## 6. Dépannage

| Problème | Solution |
|----------|----------|
| `BUILD FAILURE` sur tests Kafka | `mvn test -Dtest=KafkaMicroserviceIntegrationTest` seul ; vérifier Java 21 |
| Postman 401/403 | Ajouter headers `X-User-Id`, `X-Roles` ou JWT via Gateway |
| Postman connection refused | `docker compose up` + service sur port **8085** |
| MySQL requis pour `spring-boot:run` | Utiliser `docker compose` (pas obligatoire pour `mvn test`) |

---

## 7. Arborescence des preuves à archiver

```
service-notification/
├── docs/
│   ├── VALIDATION_TESTS.md          ← ce guide
│   ├── postman/
│   │   ├── G5-Notification-Full.postman_collection.json
│   │   └── G5-Notification-Local.postman_environment.json
│   └── reports/                     ← généré par le script
│       ├── TEST_SUMMARY_*.txt
│       └── postman-report.html      ← optionnel (Newman)
├── target/
│   ├── surefire-reports/*.xml
│   └── site/surefire-report.html
└── scripts/
    └── generate-test-reports.ps1
```
