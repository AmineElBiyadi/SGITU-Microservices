# G4 ↔ G5 — contrat REST notifications

## Canal unique

```http
POST /api/notifications/send
```

Passage par **G10**, JWT, réponse **202**.

## Règle `recipient` (obligatoire)

Chaque requête G5 contient **un seul** destinataire :

```json
"recipient": {
  "userId": "42",
  "email": "dispatcher@campus.fr"
}
```

G5 n’enrichit pas ce champ. G4 le remplit via **G3** :

1. `GET /api/users/notification-recipients?page=0&size=100` (pagination jusqu’à `total`)
2. Pour **chaque** utilisateur actif : un `POST` G5 avec le même `eventType` / `metadata` et un `recipient` différent.

## Déclenchement automatique (défaut : activé)

Après retard, déviation, panne, incident G9 confirmé, mission annulée ou clôturée :

`G5ContractNotificationService` → `G5RecipientBroadcastService` → G3 → N × POST G5 (via G10).

Configuration (`application.yml`, défaut **true**) :

- `sgitu.g5-notification.post-on-event-enabled`
- `sgitu.integration.g3-notification-recipients-enabled`

## API manuelle G4

`POST /api/notifications/send` sur G4 :

- avec `recipient` dans le body → **1** envoi G5 ;
- sans `recipient` → même logique que l’auto (liste G3, **1 POST par utilisateur**).

## Exemple payload complet

```json
{
  "notificationId": "uuid",
  "sourceService": "COORDINATION",
  "eventType": "DELAY_ALERT",
  "channel": "EMAIL",
  "recipient": {
    "userId": "1",
    "email": "ali@univ.fr"
  },
  "metadata": {
    "lineId": "L12",
    "reason": "RETARD_SIGNIFICATIF",
    "variables": {
      "vehiculeId": "BUS-07",
      "valeur": "8",
      "arret": "Campus Nord"
    }
  }
}
```

`eventType` : `DELAY_ALERT`, `ROUTE_DEVIATION`, `VEHICLE_BREAKDOWN`, `INCIDENT_CONFIRMED`, `MISSION_CANCELLED`.

## Prérequis G3

Endpoint et droits inter-service G4 documentés dans la demande équipe G3 (`GET /api/users/notification-recipients`).
