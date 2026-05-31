# Kubernetes — simulation G4 (complément Docker Compose)

**G4 seul en cluster** ; Postgres + Kafka restent en **Docker Compose** sur la machine hôte (simulation réaliste en TP).

| Mode | Usage |
|------|--------|
| **Docker Compose** | Quotidien, jour d'intégration, soutenance principale |
| **Kubernetes** | Bonus rapport / slide « déploiement cluster » |

---

## Architecture simulation

```text
┌──────────────────────── Kubernetes (namespace sgitu-g4) ────────────────────────┐
│  Deployment g4-coordination  →  Pod :8084  →  Service ClusterIP :8084          │
└───────────────────────────────┬─────────────────────────────────────────────────┘
                                │ host.docker.internal (ou host.minikube.internal)
                                ▼
┌──────────────────────── Docker Compose (infra seulement) ───────────────────────┐
│  postgres :5434   kafka :9092                                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## Prérequis

- Docker Desktop avec **Kubernetes activé**, **ou** [Minikube](https://minikube.sigs.k8s.io/)
- `kubectl` dans le PATH
- Image G4 construite localement

---

## Déploiement rapide (Windows)

Depuis `service-coordination-transport` :

```powershell
.\scripts\deploy-g4-k8s.ps1
```

Le script :

1. Démarre **postgres + kafka** (Compose, sans le conteneur G4)
2. Construit l'image `sgitu/g4-coordination:0.0.1-SNAPSHOT`
3. Charge l'image dans Minikube (si Minikube est le contexte kubectl)
4. Applique `k8s/` et attend le pod Ready
5. Lance `kubectl port-forward` sur le port **8084**

---

## Déploiement manuel (étape par étape)

### 1. Infra Postgres + Kafka (Compose)

```powershell
cd service-coordination-transport
docker compose stop g4-coordination 2>$null
docker compose up -d postgres kafka
```

Vérifier : Postgres `localhost:5434`, Kafka `localhost:9092`.

### 2. Construire l'image

```powershell
docker build -t sgitu/g4-coordination:0.0.1-SNAPSHOT .
```

### 3. (Minikube uniquement) Charger l'image

```powershell
minikube image load sgitu/g4-coordination:0.0.1-SNAPSHOT
```

Si **Docker Desktop Kubernetes** : pas besoin de `image load` (image locale visible).

### 4. Hôte pour Postgres/Kafka depuis le Pod

| Environnement | `K8S_INFRA_HOST` dans `k8s/configmap.yaml` |
|---------------|---------------------------------------------|
| Docker Desktop K8s | `host.docker.internal` |
| Minikube | `host.minikube.internal` |

Modifier puis :

```powershell
kubectl apply -k k8s/
```

### 5. Vérifier

```powershell
kubectl get pods -n sgitu-g4
kubectl get svc -n sgitu-g4
kubectl logs -n sgitu-g4 -l app=g4-coordination -f
```

Attendre `READY 1/1`.

### 6. Accéder à l'API

```powershell
kubectl port-forward -n sgitu-g4 svc/g4-coordination 8084:8084
```

Autre terminal :

```powershell
Invoke-RestMethod http://localhost:8084/api/g4/health
```

Captures rapport : `kubectl get pods`, health JSON, (optionnel) `kubectl describe deployment g4-coordination -n sgitu-g4`.

---

## Arrêter / revenir à Compose

```powershell
kubectl delete -k k8s/
docker compose up -d --build g4-coordination
```

---

## Jour d'intégration

Utiliser **Docker Compose** (réseau `sgitu-network`), pas Kubernetes seul — voir `docs/JOUR_INTEGRATION_ET_LIVRAISON_G4.md`.

Phrase soutenance :

> « L'intégration inter-groupes est validée en Docker Compose. Nous avons en complément une simulation Kubernetes (Deployment + Service) du même conteneur G4. »

---

## Fichiers

| Fichier | Rôle |
|---------|------|
| `k8s/namespace.yaml` | Namespace `sgitu-g4` |
| `k8s/configmap.yaml` | Variables non sensibles + `K8S_INFRA_HOST` |
| `k8s/secret.yaml` | `SGITU_JWT_SECRET` |
| `k8s/deployment.yaml` | Pod G4, probes `/api/g4/health` |
| `k8s/service.yaml` | Service ClusterIP port 8084 |
| `k8s/kustomization.yaml` | `kubectl apply -k k8s/` |

---

## Dépannage

| Problème | Solution |
|----------|----------|
| `ImagePullBackOff` | `docker build` + `minikube image load` |
| Pod `CrashLoopBackOff` | `kubectl logs -n sgitu-g4 -l app=g4-coordination` — souvent DB/Kafka injoignables |
| DB connection refused | Compose postgres up ? `K8S_INFRA_HOST` correct ? port **5434** |
| Kafka connection refused | `docker compose up -d kafka` ; port **9092** sur l'hôte |
| Port 8084 déjà utilisé | Arrêter Compose G4 ou autre port-forward |
