# CI/CD — G4 Coordination Transport

Chaîne automatisée dans le monorepo `SGITU-Microservices` (GitHub Actions).

---

## Fichiers

| Workflow | Fichier | Déclencheur |
|----------|---------|-------------|
| **CI** | `.github/workflows/g4-ci.yml` | Push / PR modifiant `service-coordination-transport/` |
| **CD** | `.github/workflows/g4-cd.yml` | Push sur `main` ou `master` (+ manuel) |

---

## CI — Intégration continue

À chaque push ou pull request :

1. Checkout du code  
2. Java 17 (Temurin)  
3. `./mvnw test`  
4. `./mvnw package -DskipTests` (vérifie que le JAR se construit)

**Résultat attendu :** badge vert ✅ sur GitHub → le code compile et les tests passent sans action manuelle.

---

## CD — Livraison / déploiement continu

Sur branche **main** (après CI vert) :

### Job 1 — `deploy-smoke` (déploiement smoke)

1. `docker compose up` Postgres + Kafka  
2. Build et démarrage **G4**  
3. `curl http://localhost:8084/api/g4/health` → `status: UP`  
4. `docker compose down`

→ Prouve que l’image est **déployable** automatiquement (équivalent à votre `docker compose up` local).

### Job 2 — `publish-image`

1. Build image Docker G4  
2. Push vers **GitHub Container Registry** :  
   `ghcr.io/<organisation>/<repo>/g4-coordination:latest`  
   `ghcr.io/<organisation>/<repo>/g4-coordination:<commit-sha>`

**Résultat attendu :** image versionnée prête pour intégration / K8s / serveur.

---

## Voir les pipelines sur GitHub

1. Pousser le code : `git push origin main`  
2. Onglet **Actions** du dépôt GitHub  
3. Workflows **G4 CI** et **G4 CD**

Badge README (remplacer `OWNER/REPO`) :

```markdown
![G4 CI](https://github.com/OWNER/REPO/actions/workflows/g4-ci.yml/badge.svg)
```

---

## CD Kubernetes (local ou serveur)

Le CD GitHub **ne déploie pas** sur votre Docker Desktop K8s (pas accessible depuis Internet).

| Environnement | Commande |
|---------------|----------|
| **Local K8s** | `.\scripts\deploy-g4-k8s.ps1` ou `kubectl apply -k k8s/` |
| **Cluster distant** | `kubectl apply -k k8s/` avec `KUBECONFIG` + image GHCR |

Évolution possible : secret `KUBECONFIG` + job CD `kubectl apply` (hors scope promo par défaut).

---

## Jour d'intégration

Toujours **Docker Compose** manuel sur `sgitu-network` — la CI/CD ne remplace pas la séance live avec les autres groupes.

---

## Dépannage

| Problème | Cause probable |
|----------|----------------|
| Workflow ne se lance pas | Push sans modifier `service-coordination-transport/` |
| CD ne part pas | Branche ≠ `main` / `master` |
| `publish-image` 403 | Activer **Packages** pour le repo (Settings → Actions → General → Workflow permissions) |
| Tests OK local, KO en CI | Différence de config ; lire les logs Actions |

---

## Phrase soutenance

> « Une chaîne CI exécute les tests Maven à chaque push. La CD construit l’image Docker, la publie sur GHCR et valide un déploiement smoke via Docker Compose avec health check automatique. Le déploiement Kubernetes reste une simulation locale documentée. »
