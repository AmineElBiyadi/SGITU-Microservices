## Guide complet — Tester les alertes G5 avec Postman Mock Server

---

### Pourquoi on utilise un Mock Server

G5 n'est pas encore déployé sur nos machines locales. Pour tester que nos alertes sont envoyées avec le bon format, on crée un **faux serveur G5** dans Postman qui reçoit nos requêtes et nous montre exactement ce qu'on lui envoie.

---

### Étape 1 — Ouvrir Postman et créer le Mock Server

```
1. Ouvre Postman
2. Dans le menu gauche, clique sur "Collections"
3. Clique sur le bouton "+" en haut 
   → "Blank collection"
4. Nomme-la : "G5 Mock Server"
5. Clique sur les 3 points "..." à côté 
   de la collection
6. Clique "Mock Collection"
```

---

### Étape 2 — Configurer le Mock Server

```
Tu vois un formulaire "Create a mock server"

1. Mock server name : "G5 Mock Server"

2. Dans "Add requests" :
   - Request Method : change GET → POST
   - Request URL : /api/notifications/send
   - Response Code : 202
   - Response Body : { "status": "received" }

3. Coche la case :
   ✅ "Save the mock server URL as a new 
       environment variable"

4. Clique "Create Mock Server"
```

---

### Étape 3 — Copier l'URL du Mock Server

```
Après création, Postman affiche une page avec :

"To call the mock server, follow these steps:
Send a request to the following mock server URL:"

https://9a41b459-8271-462d-8e4a-25521419392a.mock.pstmn.io

→ Copie cette URL
```

---

### Étape 4 — Modifier application.yml

Dans IntelliJ, ouvre :
```
service-analytique/src/main/resources/application.yml
```

Change la ligne G5 :
```yaml
# Avant
g5:
  notification:
    url: http://localhost:8085/api/notifications/send

# Après
g5:
  notification:
    url: https://9a41b459-8271-462d-8e4a-25521419392a.mock.pstmn.io/api/notifications/send
```

---

### Étape 5 — Insérer des données qui dépassent les seuils

Dans **MongoDB Compass** → `g8_analytics` → `incoming_events` → **Import JSON** :

```json
[
  {"sourceType": "INCIDENT", "eventType": "BREAKDOWN", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T08:00:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "DELAY", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T08:10:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "BREAKDOWN", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T08:20:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "DELAY", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T08:30:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "BREAKDOWN", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T08:40:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "DELAY", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T08:50:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "BREAKDOWN", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T09:00:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "DELAY", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T09:10:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "BREAKDOWN", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T09:20:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "DELAY", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T09:30:00Z"}, "processed": false},
  {"sourceType": "INCIDENT", "eventType": "BREAKDOWN", "zoneId": "Zone Nord", "lineId": null, "payload": {"status": "OPEN"}, "timestamp": {"$date": "2026-05-05T09:40:00Z"}, "processed": false}
]
```

Ceci insère **11 incidents** → dépasse le seuil de 10 → déclenche `HIGH_INCIDENT_VOLUME`

---

### Étape 6 — Redémarrer l'application

```
Dans IntelliJ :
1. Clique le bouton rouge ■ Stop
2. Attends 3 secondes
3. Clique le bouton vert ▶ Run
4. Attends que tu vois dans les logs :
   "Started G8AnalyticsApplication"
```

---

### Étape 7 — Déclencher le job depuis Postman

```
Ouvre Postman
→ Nouvelle requête
→ GET http://localhost:8088/test/run      
→ Clique Send
→ Tu dois voir : "Job exécuté avec succès" 200 OK
```

---

### Étape 8 — Voir les alertes reçues dans le Mock Server

```
Dans Postman :
1. Clique sur "G5 Mock Server" dans la liste gauche
2. Tu vois la page du Mock Server
3. Clique "Refresh Logs" en haut à droite
4. Tu vois la liste des requêtes reçues :
   POST /api/notifications/send - Today at ...
   POST /api/notifications/send - Today at ...
```

---

### Étape 9 — Voir le payload exact de chaque alerte

```
1. Clique sur ">" à gauche d'une ligne
   POST /api/notifications/send
2. Tu vois :
   - Request headers
   - Request body  ← clique ici
   - Response headers
   - Response body
3. Dans Request body, clique sur "▶" 
   pour voir le JSON complet
```

---

### Ce que tu dois voir dans le payload

```json
{
  "sourceService": "G8_ANALYTICS",
  "eventType": "INCIDENT_ZONE_RISK",
  "channel": "EMAIL",
  "priority": "HIGH",
  "notificationId": "uuid-xxx",
  "recipient": {
    "userId": "op-01",
    "email": "operateur@sgitu.ma"
  },
  "metadata": {
    "severity": "CRITICAL",
    "targetAudience": "OPERATORS",
    "value": 3,
    "threshold": 3,
    "zoneId": "GLOBAL",
    "period": "2026-05",
    "statId": "INC_05"
  }
}
```

---

