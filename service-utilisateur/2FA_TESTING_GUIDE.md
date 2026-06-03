# 🧪 Guide de Test - Vérification Email 2FA

## 📋 Prérequis

- Service utilisateur déployé (Docker ou Kubernetes)
- Redis accessible
- Kafka accessible
- Postman ou curl installé

---

## 🔄 Rebuild et Redéploiement (Important après modification du code)

### 🚨 Si tu viens de modifier le code source

Après toute modification du code (ajout 2FA, bug fix, etc.), tu dois rebuild l'image Docker et redéployer :

#### Méthode Complète (PowerShell)

```powershell
cd service-utilisateur

# 1. Compiler le nouveau JAR
./mvnw clean package -DskipTests

# 2. Construire la nouvelle image Docker
docker build -t user-service:latest .

# 3. Appliquer le deployment
kubectl apply -f k8s/user-service-deployment.yaml

# 4. Redémarrer les pods pour utiliser la nouvelle image
kubectl rollout restart deployment/user-service -n sgitu

# 5. Attendre que le nouveau déploiement soit prêt
kubectl rollout status deployment/user-service -n sgitu --timeout=180s

# 6. Vérifier que les nouveaux pods sont en cours d'exécution
kubectl get pods -n sgitu -l app=user-service
```

#### Méthode Complète (Git Bash)

```bash
cd service-utilisateur

# 1. Compiler le nouveau JAR
./mvnw clean package -DskipTests

# 2. Construire la nouvelle image Docker
docker build -t user-service:latest .

# 3. Appliquer le deployment
kubectl apply -f k8s/user-service-deployment.yaml

# 4. Redémarrer les pods
kubectl rollout restart deployment/user-service -n sgitu

# 5. Attendre le déploiement
kubectl rollout status deployment/user-service -n sgitu --timeout=180s

# 6. Vérifier les pods
kubectl get pods -n sgitu -l app=user-service
```

#### Script Automatisé (Git Bash uniquement)

```bash
# Utiliser le script tout-en-un
bash rebuild-and-deploy.sh
```

---

## 🚀 Démarrage Rapide (Première Installation)

### Option 1 : Docker Compose
```powershell
cd service-utilisateur
docker-compose up -d
```

### Option 2 : Kubernetes - Première Installation (PowerShell)
```powershell
cd service-utilisateur\k8s
.\deploy.ps1

# Attendre que tous les pods soient prêts
kubectl get pods -n sgitu

# Port-forward pour accéder au service
kubectl port-forward svc/user-service 8083:8083 -n sgitu
```

### Option 3 : Kubernetes - Première Installation (Git Bash)
```bash
cd service-utilisateur/k8s
bash deploy.sh

# Attendre que tous les pods soient prêts
kubectl get pods -n sgitu

# Port-forward pour accéder au service
kubectl port-forward svc/user-service 8083:8083 -n sgitu
```

---

## 🧪 Scénarios de Test

### ✅ Test 1 : Inscription avec vérification d'email

**Endpoint** : `POST http://localhost:8083/api/users`

**Body** :
```json
{
  "email": "test@example.com",
  "password": "Password123!",
  "role": "ROLE_PASSENGER",
  "profile": {
    "firstName": "John",
    "lastName": "Doe",
    "phone": "0600000000",
    "address": "123 Rue Example, Casablanca",
    "birthDate": "1990-01-01"
  }
}
```

**Réponse attendue** :
```json
{
  "id": 1,
  "email": "test@example.com",
  "active": false,  ⬅️ IMPORTANT : Le compte est inactif
  "roles": ["ROLE_PASSENGER"],
  "profile": {
    "firstName": "John",
    "lastName": "Doe",
    ...
  }
}
```

**Vérifications** :
1. ✅ `active: false` dans la réponse
2. ✅ Code de vérification loggé dans les logs du service :
   ```
   🔐 VERIFICATION CODE FOR test@example.com : 123456
   ```
3. ✅ Événement Kafka publié sur le topic `user-events`

---

### 🔍 Test 2 : Récupérer le code de vérification depuis Redis

#### Via Docker Compose :
```powershell
docker exec -it users-redis redis-cli
```

#### Via Kubernetes :
```powershell
# Trouver le pod Redis
kubectl get pods -n sgitu | findstr redis

# Se connecter au pod Redis
kubectl exec -it redis-XXXXXX -n sgitu -- redis-cli
```

#### Commandes Redis :
```redis
# Récupérer le code pour un email spécifique
GET email_verification:test@example.com
# Résultat : "123456"

# Lister toutes les clés de vérification
KEYS email_verification:*

# Vérifier le temps restant avant expiration (en secondes)
TTL email_verification:test@example.com
# Résultat : 894 (environ 15 minutes = 900 secondes)

# Quitter Redis
exit
```

---

### ✅ Test 3 : Tentative de connexion AVANT vérification

**Endpoint** : `POST http://localhost:8083/api/auth/login`

**Body** :
```json
{
  "email": "test@example.com",
  "password": "Password123!"
}
```

**Réponse attendue** :
```
Status: 403 Forbidden
{
  "error": "Compte desactive - contactez un administrateur"
}
```

✅ **Résultat attendu** : La connexion est refusée car le compte n'est pas encore vérifié.

---

### ✅ Test 4 : Vérifier l'email avec un MAUVAIS code

**Endpoint** : `POST http://localhost:8083/api/auth/verify-email`

**Body** :
```json
{
  "email": "test@example.com",
  "code": "999999"
}
```

**Réponse attendue** :
```
Status: 400 Bad Request
{
  "error": "Code invalide ou expiré"
}
```

---

### ✅ Test 5 : Vérifier l'email avec le BON code

**Endpoint** : `POST http://localhost:8083/api/auth/verify-email`

**Body** :
```json
{
  "email": "test@example.com",
  "code": "123456"
}
```

**Réponse attendue** :
```json
{
  "message": "Email vérifié avec succès. Vous pouvez maintenant vous connecter."
}
```

**Vérifications** :
1. ✅ Le compte est maintenant actif
2. ✅ Le code est supprimé de Redis :
   ```redis
   GET email_verification:test@example.com
   # Résultat : (nil)
   ```
3. ✅ Événement Kafka publié : `USER_ACTIVATED`

---

### ✅ Test 6 : Connexion APRÈS vérification

**Endpoint** : `POST http://localhost:8083/api/auth/login`

**Body** :
```json
{
  "email": "test@example.com",
  "password": "Password123!"
}
```

**Réponse attendue** :
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "test@example.com",
  "roles": ["ROLE_PASSENGER"]
}
```

✅ **Résultat attendu** : La connexion fonctionne et retourne les tokens JWT.

---

### ✅ Test 7 : Renvoyer le code de vérification

#### Scénario : L'utilisateur n'a pas reçu le premier code

**Endpoint** : `POST http://localhost:8083/api/auth/resend-code`

**Body** :
```json
{
  "email": "test2@example.com"
}
```

**Réponse attendue** :
```json
{
  "message": "Nouveau code de vérification envoyé à votre email."
}
```

**Vérifications** :
1. ✅ Nouveau code généré et loggé
2. ✅ Ancien code remplacé dans Redis
3. ✅ Nouvel événement Kafka publié

---

### ❌ Test 8 : Renvoyer code pour un compte DÉJÀ vérifié

**Endpoint** : `POST http://localhost:8083/api/auth/resend-code`

**Body** :
```json
{
  "email": "test@example.com"
}
```

**Réponse attendue** :
```
Status: 400 Bad Request
{
  "error": "Compte déjà vérifié"
}
```

---

### ❌ Test 9 : Vérifier un code expiré

**Scénario** : Attendre 15 minutes après l'inscription

**Endpoint** : `POST http://localhost:8083/api/auth/verify-email`

**Body** :
```json
{
  "email": "test@example.com",
  "code": "123456"
}
```

**Réponse attendue** :
```
Status: 400 Bad Request
{
  "error": "Code invalide ou expiré"
}
```

**Solution** : Utiliser `/auth/resend-code` pour obtenir un nouveau code.

---

## 📊 Vérification des événements Kafka

### Docker Compose :
```powershell
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning \
  --property print.key=true
```

### Kubernetes :
```powershell
kubectl exec -it kafka-XXXXXX -n sgitu -- /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning
```

**Événements attendus** :

1. **Après inscription** :
```json
{
  "eventType": "EMAIL_VERIFICATION",
  "userId": "1",
  "email": "test@example.com",
  "username": "John Doe",
  "verificationCode": "123456",
  "timestamp": "2026-06-02T10:30:00Z"
}
```

2. **Après vérification** :
```json
{
  "eventType": "WELCOME",
  "userId": "1",
  "email": "test@example.com",
  "username": "John Doe",
  "timestamp": "2026-06-02T10:35:00Z"
}
```

3. **Événement USER_ACTIVATED** :
```json
{
  "userId": 1,
  "email": "test@example.com",
  "eventType": "USER_ACTIVATED",
  "timestamp": "2026-06-02T10:35:00Z"
}
```

---

## 🐛 Dépannage

### Problème : Après modification du code, l'ancien comportement persiste

**Symptôme** : Tu as modifié le code mais les changements ne sont pas pris en compte (ex: `active: true` au lieu de `false`).

**Cause** : Les pods Kubernetes utilisent encore l'ancienne image Docker.

**Solution** :
```powershell
cd service-utilisateur

# 1. Recompiler
./mvnw clean package -DskipTests

# 2. Rebuild l'image Docker
docker build -t user-service:latest .

# 3. Redéployer
kubectl apply -f k8s/user-service-deployment.yaml
kubectl rollout restart deployment/user-service -n sgitu
kubectl rollout status deployment/user-service -n sgitu

# 4. Vérifier les nouveaux pods
kubectl get pods -n sgitu -l app=user-service
```

---

### Problème : Le code n'apparaît pas dans les logs

**Solution** :
```powershell
# Docker Compose
docker logs users-app -f --tail 100

# Kubernetes
kubectl logs -f deployment/user-service -n sgitu
```

Recherchez : `🔐 VERIFICATION CODE FOR`

---

### Problème : Redis ne stocke pas le code

**Vérification** :
1. Redis est bien démarré :
   ```powershell
   # Docker
   docker ps | findstr redis
   
   # Kubernetes
   kubectl get pods -n sgitu | findstr redis
   ```

2. Connexion depuis le service utilisateur :
   ```powershell
   # Vérifier les logs pour des erreurs Redis
   kubectl logs deployment/user-service -n sgitu | findstr -i redis
   ```

---

### Problème : "imagePullPolicy" ou image introuvable

**Solution** : Vérifie que le fichier `k8s/user-service-deployment.yaml` contient :
```yaml
containers:
- name: user-service
  image: user-service:latest
  imagePullPolicy: Never  # Important pour utiliser l'image locale
```

Si ce n'est pas le cas, modifie le fichier et redéploie :
```powershell
kubectl apply -f k8s/user-service-deployment.yaml
kubectl rollout restart deployment/user-service -n sgitu
```

---

### Problème : Kafka ne reçoit pas les événements

**Vérification** :
```powershell
# Lister les topics
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list

# Vérifier que "user-events" existe
```

---

## 📝 Collection Postman

Créer une collection avec ces requêtes dans l'ordre :

1. **Inscription** → `POST /api/users`
2. **Login (échoue)** → `POST /api/auth/login`
3. **Vérifier Email** → `POST /api/auth/verify-email`
4. **Login (réussit)** → `POST /api/auth/login`
5. **Renvoyer Code** → `POST /api/auth/resend-code`

**Variables Postman** :
- `BASE_URL` = `http://localhost:8083/api`
- `EMAIL` = `test@example.com`
- `CODE` = `123456` (à récupérer depuis Redis)

---

## ✅ Checklist de Validation

- [ ] **Code source modifié et deployé**
  - [ ] Compilation Maven réussie
  - [ ] Image Docker reconstruite
  - [ ] Deployment Kubernetes mis à jour
  - [ ] Nouveaux pods en cours d'exécution
- [ ] Inscription crée un compte `active: false`
- [ ] Code apparaît dans les logs
- [ ] Code stocké dans Redis avec TTL 15min
- [ ] Login refusé avant vérification (403)
- [ ] Vérification avec mauvais code échoue (400)
- [ ] Vérification avec bon code active le compte
- [ ] Login réussit après vérification (200 + JWT)
- [ ] Code supprimé de Redis après utilisation
- [ ] Événements Kafka publiés correctement
- [ ] Renvoyer code fonctionne pour compte non vérifié
- [ ] Renvoyer code échoue pour compte déjà vérifié

---

## 🎯 Résumé

| Test | Endpoint | Status Attendu | Active |
|------|----------|----------------|--------|
| 1. Inscription | `POST /users` | 201 Created | false |
| 2. Login avant vérification | `POST /auth/login` | 403 Forbidden | - |
| 3. Vérification (mauvais code) | `POST /auth/verify-email` | 400 Bad Request | false |
| 4. Vérification (bon code) | `POST /auth/verify-email` | 200 OK | true |
| 5. Login après vérification | `POST /auth/login` | 200 OK + JWT | - |
| 6. Renvoyer code (non vérifié) | `POST /auth/resend-code` | 200 OK | false |
| 7. Renvoyer code (déjà vérifié) | `POST /auth/resend-code` | 400 Bad Request | true |

---

## 📞 Support

Pour toute question, vérifier :
1. Les logs du service utilisateur
2. Les données dans Redis
3. Les événements Kafka
4. La documentation complète : `2FA_EMAIL_VERIFICATION.md`

---

**Temps de test estimé** : 20-30 minutes pour tous les scénarios.
