# Guide de Déploiement Kubernetes - Service Notification (G5)

Ce guide détaille pas à pas comment déployer le `service-notification` et ses dépendances (MySQL & Kafka) dans votre cluster Kubernetes local (Docker Desktop) sans aucune erreur.

> **Note de sécurité** : Toutes les valeurs secrètes (base de données, JWT, Twilio, Gmail) ont été scrupuleusement encodées en Base64 et correspondent **exactement** à votre fichier `.env`.

---

## Étape 1 : Nettoyage (Optionnel mais recommandé)
Si vous aviez des conteneurs Docker normaux qui tournent sur les mêmes ports (3314, 9092, 8085), il vaut mieux les arrêter pour éviter les conflits avec Kubernetes :
```bash
docker compose down
```

## Étape 2 : Construction de l'image Docker locale
Kubernetes a besoin d'avoir l'image de votre application. Ouvrez un terminal (PowerShell) dans le dossier `service-notification` et exécutez :
```bash
docker build -t sgitu/notification-service:1.0.0 .
```

## Étape 3 : Création du Secret Firebase
Pour que les notifications Push fonctionnent, K8s doit absorber votre fichier JSON d'authentification. **Vérifiez que le fichier `firebase-adminsdk.json` est bien présent dans votre dossier `service-notification`**, puis exécutez exactement cette commande :
```bash
kubectl create secret generic firebase-secret --from-file=firebase-adminsdk.json=./firebase-adminsdk.json
```
*(Si le secret existe déjà et que ça bloque, supprimez-le d'abord avec `kubectl delete secret firebase-secret` puis recréez-le).*

## Étape 4 : Déploiement de l'Infrastructure (MySQL, Kafka, Service)
C'est le moment de tout lancer ! Vos 6 fichiers YAML sont prêts et contiennent les bonnes variables. Lancez :
```bash
kubectl apply -f k8s/
```
Vous devriez voir ceci apparaître à l'écran :
```
configmap/notification-config created
secret/notification-secret created
deployment.apps/notification-service created
service/notification-service created
deployment.apps/mysql-notification created
service/mysql-notification created
deployment.apps/kafka-broker created
service/kafka-broker created
```

## Étape 5 : Surveillance du démarrage (Très important)
Kubernetes ne démarre pas tout en une seconde. MySQL et Kafka doivent être "Prêts" avant que le service Spring Boot ne puisse s'y connecter sans crasher.

Tapez cette commande pour voir l'évolution en temps réel :
```bash
kubectl get pods -w
```
*(Pour quitter cette vue en direct, faites `Ctrl+C`).*

Attendez que les **3 pods** affichent `1/1` dans la colonne `READY` et `Running` dans la colonne `STATUS`.
Si le pod `notification-service` crashe (CrashLoopBackOff) au début, **c'est normal !** Il réessayera automatiquement dès que MySQL et Kafka seront bien réveillés. Laissez-lui 1 ou 2 minutes.

## Étape 6 : Vérification des logs
Une fois que le pod `notification-service` est `Running`, vérifiez que Tomcat a bien démarré sans erreur (et sans `UnknownHostException`) :
```bash
kubectl logs -l app=notification-service
```
Vous devriez voir le logo Spring Boot et le message de succès à la fin !

## Étape 7 : Tester l'application (Port-Forward)
L'application tourne parfaitement, mais elle est enfermée dans le cluster Kubernetes. Pour y accéder depuis votre ordinateur Windows, ouvrez le port :
```bash
kubectl port-forward svc/notification-service 8085:8085
```
**Laissez cette fenêtre de terminal ouverte !**

Vous pouvez maintenant ouvrir votre navigateur sur :
- **[http://localhost:8085/api/notifications/health](http://localhost:8085/api/notifications/health)** (Devrait afficher "UP")
- **[http://localhost:8085/actuator/prometheus](http://localhost:8085/actuator/prometheus)** (Si Actuator/Prometheus est activé)
- **[http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)** (Pour tester vos requêtes de notification)

Bravo, votre microservice est déployé de manière 100% Cloud-Native !
