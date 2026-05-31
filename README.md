# SGITU-Microservices

## Aperçu

SGITU-Microservices est une application complète basée sur des microservices conçue pour gérer les systèmes de transport urbain et de billetterie. L'architecture suit les principes modernes des microservices avec des services indépendants communiquant via une passerelle API.

## Architecture

Le système se compose de plusieurs microservices, chacun gérant un domaine spécifique :

- **API Gateway** : Point d'entrée central pour toutes les demandes des clients, gérant le routage, l'authentification et l'équilibrage de charge
- **Service-Abonnement** : Gère les abonnements et plans des utilisateurs
- **Service-Analytique** : Fournit des capacités d'analyse et de reporting
- **Service-Billetterie** : Gère la réservation et la gestion des billets
- **Service-Coordination-Transport** : Coordonne les horaires et itinéraires de transport
- **Service-Gestion-Incidents** : Gère le signalement et la résolution des incidents
- **Service-Notification** : Gère les notifications et alertes
- **Service-Paiement** : Traite les paiements et transactions financières
- **Service-Suivi-Vehicule** : Suit les emplacements et statuts des véhicules
- **Service-Utilisateur** : Gère les comptes et profils des utilisateurs

## Prérequis

- Docker et Docker Compose
- Git

## Démarrage

1. Cloner le dépôt :
   ```bash
   git clone https://github.com/AmineElBiyadi/SGITU-Microservices.git
   cd SGITU-Microservices
   ```

2. Démarrer les services en utilisant Docker Compose :
   ```bash
   docker-compose up -d
   ```

## Développement

Chaque service est contenu dans son propre répertoire. Pour développer un service spécifique :

1. Naviguer vers le répertoire du service
2. Suivre le README spécifique au service (si disponible)
3. Exécuter le service en mode développement

## Contribution

1. Forker le dépôt
2. Créer une branche de fonctionnalité (`git checkout -b feature/AmazingFeature`)
3. Commiter vos changements (`git commit -m 'Ajouter une fonctionnalité incroyable'`)
4. Pousser vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request