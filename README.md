# SGITU - Microservices

SGITU est un systeme distribue de gestion intelligente des transports urbains. Le projet regroupe les microservices G1 a G10 dans une architecture Docker commune, avec API Gateway, securite JWT, bases de donnees, Kafka, monitoring Prometheus/Grafana et simulation Kubernetes.

## Architecture

| Groupe | Microservice | Role |
|---|---|---|
| G1 | `service-billetterie` | Billetterie dematerialisee |
| G2 | `service-abonnement` | Abonnements et plans |
| G3 | `user-service` / `g3-user-service` | Utilisateurs, roles, mots de passe, emission JWT |
| G4 | `g4-coordination` | Coordination transport |
| G5 | `notification-service` | Notifications |
| G6 | `payment-service` | Paiements |
| G7 | `g7-service` | Suivi des vehicules |
| G8 | `g8-analytics-service` + `g8-ml-service` | Analyse, ingestion, predictions ML |
| G9 | `service-gestion-incidents` | Incidents |
| G10 | `api-gateway` | API Gateway et securite transverse |

G10 est le point d'entree HTTP principal :

```text
Client / Postman / Frontend -> http://localhost:8080 -> G10 -> G1..G9
```

## Prerequis

- Docker Desktop
- Docker Compose
- Git
- Java 17 ou 21 si vous lancez certains services sans Docker
- Maven ou Maven Wrapper inclus dans les services

## Configuration

Copier le fichier d'exemple :

```powershell
cp .env.example .env
```

Verifier au minimum :

```text
JWT_SECRET
G4_JWT_SECRET_B64
GRAFANA_ADMIN_PASSWORD
MYSQL_ROOT_PASSWORD
MYSQL_PASSWORD
users_DB_NAME / users_DB_USER / users_DB_PASSWORD
abonnement_DB_NAME / abonnement_DB_USER / abonnement_DB_PASSWORD
billetterie_MONGO_ROOT_USERNAME / billetterie_MONGO_ROOT_PASSWORD
```

Le meme `JWT_SECRET` doit etre partage entre G3, G10 et les services qui valident le JWT.

## Lancement local complet

Depuis la racine du projet :

```powershell
docker compose up -d --build
```

Verifier l'etat :

```powershell
docker compose ps
```

Voir les logs Gateway :

```powershell
docker compose logs -f api-gateway
```

Arreter l'environnement :

```powershell
docker compose down
```

Arreter avec suppression des volumes :

```powershell
docker compose down -v
```

## URLs utiles

| Service | URL |
|---|---|
| API Gateway G10 | `http://localhost:8080` |
| Health Gateway | `http://localhost:8080/actuator/health` |
| Swagger Gateway | `http://localhost:8080/swagger-ui.html` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

Identifiants Grafana par defaut :

```text
admin / admin123
```

Le mot de passe peut etre change avec `GRAFANA_ADMIN_PASSWORD` dans `.env`.

## Docker Compose global

Le fichier `docker-compose.yml` lance :

- les microservices G1 a G10 ;
- les bases de donnees necessaires : PostgreSQL, MySQL, MongoDB ;
- Redis pour la blacklist des tokens G10 ;
- Kafka pour les communications asynchrones ;
- Prometheus et Grafana pour le monitoring.

Le projet n'utilise pas Eureka ni Config Server dans cette version. La decouverte locale des services est faite par les noms Docker dans le reseau `sgitu-network`.

## Securite JWT

G3 est responsable des utilisateurs, roles, mots de passe et emission des JWT.

G10 :

- valide les JWT ;
- verifie les roles ;
- bloque les routes protegees sans token ;
- propage les headers utilisateur vers les services cibles.

Headers propages par G10 :

```text
X-User-Id
X-User-Email
X-Roles
X-User-Role
X-Source-Group
X-Correlation-Id
```

## Tests Postman

Les collections de tests sont dans :

```text
api-gateway/docs/
```

Collections importantes :

- `SGITU_G10_Postman_Collection.json`
- `G10_G3_G8_Integration_Postman_Collection.json`
- `G10_Gateway_G1_Billetterie_Postman_Collection.json`
- `G10_Gateway_G5_Notification_Postman_Collection.json`
- `G10_Gateway_G8_Relation_Postman_Collection.json`
- `G10_MultiGroup_EndToEnd_Postman_Collection.json`

Scenarios a verifier :

- health G10 : `200` ;
- login G3 via G10 : `200 + JWT` ;
- route protegee sans JWT : `401` ;
- role insuffisant : `403` ;
- analytics avec `ROLE_OPERATOR` : `200` ;
- notification via G10 : `202` ;
- microservice cible arrete : `503`.

## Tests Maven

Tests G10 :

```powershell
cd api-gateway
.\mvnw.cmd test
```

Packaging global par service :

```powershell
cd service-utilisateur
mvn -B -DskipTests package
```

Repeter selon le service a verifier.

## Monitoring

Prometheus lit la configuration :

```text
prometheus.yml
```

Grafana charge les dashboards depuis :

```text
monitoring/grafana/dashboards/
```

Dashboard principal :

```text
SGITU - Vue Globale Tous les Groupes
```

Verifier les targets Prometheus :

```text
http://localhost:9090/targets
```

## Kubernetes

Les manifests globaux sont dans :

```text
k8s/global/
```

Deploiement :

```powershell
kubectl apply -k k8s/global
```

Verification :

```powershell
kubectl get pods -n sgitu
kubectl get svc -n sgitu
```

Port-forward Gateway :

```powershell
kubectl port-forward -n sgitu svc/api-gateway 8080:8080
```

Port-forward monitoring :

```powershell
kubectl port-forward -n sgitu svc/prometheus 9090:9090
kubectl port-forward -n sgitu svc/grafana 3000:3000
```

Suppression :

```powershell
kubectl delete -k k8s/global
```

## CI/CD GitHub Actions

Workflow global :

```text
.github/workflows/sgitu-global-ci-cd.yml
```

Il verifie :

- packaging Maven des microservices ;
- tests automatises de G10 ;
- tests complets optionnels via `workflow_dispatch` ;
- validation Docker Compose ;
- validation Prometheus ;
- rendu Kubernetes ;
- build Docker des images.

Declenchement :

- push sur `main`, `master`, `develop` ;
- pull request vers `main`, `master`, `develop` ;
- lancement manuel depuis GitHub Actions.

## Structure du projet

```text
SGITU-Microservices
|-- api-gateway
|-- service-abonnement
|-- service-analytique
|-- service-billetterie
|-- service-coordination-transport
|-- service-gestion-incidents
|-- service-notification
|-- service-paiement
|-- service-suivi-vehicule
|-- service-utilisateur
|-- k8s
|-- monitoring
|-- docker-compose.yml
|-- prometheus.yml
|-- .env.example
`-- README.md
```

