package com.agileflow.api_gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGITU — API Gateway G10")
                        .version("1.0.0")
                        .description("""
                                **API Gateway & Sécurité** — Groupe 10 SGITU
                                
                                Ce service est le point d'entrée unique du système SGITU.
                                Il gère :
                                - L'authentification JWT (register, login, logout, refresh)
                                - Le routage vers les 9 microservices métier (G1–G9)
                                - Le contrôle d'accès par rôle (RBAC)
                                - La propagation des Correlation IDs
                                
                                **Rôles disponibles :** ROLE_PASSENGER, ROLE_STUDENT, ROLE_DRIVER,
                                ROLE_OPERATOR, ROLE_TECHNICIAN, ROLE_STAFF, ROLE_ADMIN
                                """)
                        .contact(new Contact()
                                .name("Groupe 10 — KHALID WISSAL, HARISS HOUSSAM, ES-SERRAR ACHRAF, SADIKI ABDERRAHIM")
                        )
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Développement local"),
                        new Server().url("http://api-gateway:8080").description("Docker")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Entrez votre access token JWT (sans le préfixe 'Bearer ')")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
