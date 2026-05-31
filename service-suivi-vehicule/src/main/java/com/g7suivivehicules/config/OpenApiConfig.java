package com.g7suivivehicules.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "G7 - API Suivi Véhicules",
                version = "1.0",
                description = "Microservice de suivi GPS en temps réel et détection automatique d'anomalies " +
                        "(vitesse excessive, déviation d'itinéraire, retard horaire, freinage brusque, " +
                        "immobilisation anormale). Intégration Kafka vers Groupes 4 et 9. " +
                        "Projet SGITU — Architecture Microservices.",
                contact = @Contact(
                        name = "Groupe G7 - SGITU",
                        email = "g7@sgitu.dz"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8087", description = "Serveur local de développement")
        }
)
public class OpenApiConfig {
    // Configuration automatique via annotations
}
