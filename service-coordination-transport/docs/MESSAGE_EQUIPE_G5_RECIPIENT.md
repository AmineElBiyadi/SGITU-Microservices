# Message équipe — champ `recipient` hors périmètre G4

Texte prêt à envoyer (G5, encadrant, chef de projet).

---

Bonjour,

Côté **G4 — Coordination des transports**, nous implémentons le contrat G4 ↔ G5 :

- détection des événements (retard, déviation, panne, incident confirmé, mission annulée / fin de service) ;
- envoi vers G5 via `POST /api/notifications/send` (passage par G10) avec le **contenu métier** du contrat :
  - `sourceService` : `COORDINATION`
  - `eventType` : `DELAY_ALERT`, `ROUTE_DEVIATION`, `VEHICLE_BREAKDOWN`, `INCIDENT_CONFIRMED`, `MISSION_CANCELLED`
  - `metadata.lineId`, `metadata.reason`, `metadata.variables` (`vehiculeId`, `valeur`, `arret`, `lieu`, etc.)

**Le champ `recipient` (`userId`, `email`) est requis dans le payload global du contrat, mais il est hors périmètre du microservice G4.**

G4 n’est **pas** le référentiel des abonnés voyageurs : notre rôle est la **coordination du réseau** et la **description structurée de l’événement**, pas la gestion des listes d’emails par ligne.

**Relation technique :** uniquement `POST /api/notifications/send` (REST via G10) — pas d’autre canal.

**Proposition :**

1. **G4** transmet le JSON métier **sans** `recipient` via ce POST.
2. **G5** (ou G2 / G3) **complète** `recipient` avant envoi SMTP.

Merci de confirmer :

1. Qui prend en charge `recipient` en production ?
2. G5 dispose-t-il des **templates** du §4 (`DELAY_ALERT` + `RETARD_SIGNIFICATIF`, etc.) ?

Cordialement,  
Équipe G4 — Coordination des transports

---

## Version courte (Teams / WhatsApp)

> G4 envoie le JSON métier du contrat G5 (eventType, reason, variables). Le `recipient` n’est pas géré par G4 (pas notre référentiel abonnés). Qui le remplit en prod : G5, G2 ou G3 ? G5 a-t-il les templates DELAY_ALERT / ROUTE_DEVIATION du contrat ?
