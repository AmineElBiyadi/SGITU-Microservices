# Résilience G4 — alignement axe prof « Chaos Monkey »

## Stratégie

| Intégration | Comportement si voisin down | Technologie |
|-------------|----------------------------|-------------|
| **G5** notifications | `202` + statut **`DEGRADED`** | Resilience4j `@CircuitBreaker` + fallback |
| **G7** statut véhicule | Map mock `INCONNU` | try/catch RestClient |
| **G3** validation chauffeur | Mode non strict : ignore ; strict : 400 | config `g3-validation-strict` |
| **G1** Kafka lifecycle | Log WARN, mission locale OK | try/catch publish |
| **G9** Kafka incident | Log WARN, message rejeté | try/catch consumer |

## Démo Chaos Monkey (soutenance)

1. Démarrer G4 : `docker compose up -d`
2. **Couper G5** ou G10 (ou URL invalide dans `.env`)
3. Postman : `POST /api/notifications/send` avec token DISPATCHER
4. Réponse attendue : **`status: DEGRADED`** (pas 500)
5. Vérifier `GET /api/g4/logs` → entrée `NOTIFICATION -> DEGRADED`

## Configuration Resilience4j

Voir `application.yml` → `resilience4j.circuitbreaker.instances.g5Notification`.

Actuator : `/actuator/health` peut exposer l'état du circuit breaker G5.

## Perspectives

- Retry G5 avec file d'attente
- Resilience4j sur G3 validation (optionnel)
- Stockage local des notifications DEGRADED pour renvoi ultérieur
