# SGITU - Groupe 10 - API Gateway & Securite

Ce microservice est le point d'entree HTTP du systeme SGITU. Il valide les JWT emis par le service utilisateur G3, applique le controle d'acces par roles, route les requetes vers les microservices metier et propage le contexte utilisateur.

## Prerequis

- Java 17+
- Docker Desktop
- Maven Wrapper inclus dans le projet
- Reseau Docker commun :

```powershell
docker network create sgitu-network
```

## Lancer G10 avec Docker Compose

Depuis le dossier `api-gateway` :

```powershell
docker compose up -d --build
```

Services exposes :

- API Gateway : `http://localhost:8080`
- Swagger : `http://localhost:8080/swagger-ui.html`
- Health : `http://localhost:8080/actuator/health`
- Prometheus : `http://localhost:9090`
- Grafana : `http://localhost:3000`

Identifiants Grafana par defaut :

```text
admin / admin123
```

## Lancer les tests automatises

```powershell
.\mvnw.cmd test
```

Les tests couvrent notamment :

- chargement du contexte Spring Boot ;
- validation JWT ;
- controle d'acces par roles ;
- erreurs 401, 403, 404, 503 ;
- propagation des headers ;
- routage et fallback gateway.

## Tests Postman

Les collections Postman sont dans :

```text
docs/
```

Collections principales :

- `G10_Gateway_G3_Relation_Postman_Collection.json`
- `G10_Gateway_G8_Relation_Postman_Collection.json`
- `G10_Gateway_G5_Notification_Postman_Collection.json`
- `G10_Gateway_G1_Billetterie_Postman_Collection.json`
- `G10_Integration_G2_G3_G4_G7_G8_Postman_Collection.json`
- `G10_Integration_G3_G8_G5_Network_Postman_Collection.json`

Scenarios importants :

- login via G10 vers G3 ;
- acces sans JWT : 401 ;
- role insuffisant : 403 ;
- acces autorise : 200 ;
- microservice cible arrete : 503 ;
- verification des logs avec `X-Correlation-Id`.

## Logs

```powershell
docker compose logs -f api-gateway
docker compose logs -f prometheus
docker compose logs -f grafana
```

Les logs de la Gateway affichent notamment :

- methode HTTP ;
- chemin appele ;
- utilisateur ;
- roles ;
- `X-Correlation-Id` ;
- statut de reponse ou erreur de routage.

## Variables importantes

Les variables peuvent etre definies dans un fichier `.env` ou dans l'environnement :

```text
JWT_SECRET
TOKEN_BLACKLIST_ENABLED
REDIS_HOST
REDIS_PORT
GATEWAY_CONNECT_TIMEOUT_MS
GATEWAY_RESPONSE_TIMEOUT
G8_ANALYTICS_URI
G8_ML_URI
```

G10 ne stocke pas de base utilisateurs. Il valide les tokens et consulte Redis pour refuser les tokens revoques lorsque la blacklist est active.

## Kubernetes

Les manifests G10 sont dans :

```text
k8s/
```

Verification :

```powershell
kubectl kustomize k8s
kubectl apply -k k8s --dry-run=client
```

Application :

```powershell
kubectl apply -k k8s
```

Services NodePort :

- Gateway : `30080`
- Prometheus : `30090`
- Grafana : `30300`

## Arret

```powershell
docker compose down
```
