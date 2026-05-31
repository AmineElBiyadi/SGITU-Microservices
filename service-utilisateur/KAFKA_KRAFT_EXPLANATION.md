# Intégration de Kafka avec KRaft

Ce document explique comment Kafka est intégré dans le `service-utilisateur` en utilisant le mode **KRaft** (Kafka Raft), ce qui permet de se passer de Zookeeper.

## Pourquoi KRaft ?

Auparavant, Kafka nécessitait un cluster Zookeeper séparé pour gérer les métadonnées (élections de leaders, configuration des topics, etc.). KRaft simplifie l'architecture en intégrant la gestion des métadonnées directement dans Kafka via un algorithme de consensus (Raft).

**Avantages :**
- Architecture plus simple (un seul type de service à gérer).
- Meilleure scalabilité.
- Démarrage plus rapide du cluster.

## Configuration Docker

Le service Kafka est ajouté dans le fichier `docker-compose.yml`. Voici les points clés de la configuration :

- **Image** : `apache/kafka:latest` (image officielle).
- **KAFKA_PROCESS_ROLES** : `broker,controller` signifie que le noeud agit à la fois comme courtier de données et comme contrôleur de métadonnées.
- **KAFKA_CONTROLLER_QUORUM_VOTERS** : Définit les noeuds qui participent au quorum KRaft (ici, un seul noeud sur le port 9093).
- **KAFKA_LISTENERS** : Définit les interfaces réseau (port 9092 pour les données, port 9093 pour le contrôleur).

## Configuration Spring Boot

Dans `application.properties`, Kafka est configuré pour se connecter au broker :

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=user-service-group
# ... sérialiseurs et désérialiseurs par défaut
```

## Comment l'utiliser dans le code ?

### Envoyer un message (Producer)

```java
@Autowired
private KafkaTemplate<String, String> kafkaTemplate;

public void sendMessage(String topic, String message) {
    kafkaTemplate.send(topic, message);
}
```

### Recevoir un message (Consumer)

```java
@KafkaListener(topics = "nom-du-topic", groupId = "user-service-group")
public void listen(String message) {
    System.out.println("Message reçu : " + message);
}
```

## Vérification du fonctionnement

Pour vérifier que Kafka fonctionne correctement dans Docker :

1. Démarrez les services : `docker-compose up -d`
2. Consultez les logs de Kafka : `docker logs kafka`
3. Vérifiez que l'application Spring Boot se connecte bien au démarrage.
