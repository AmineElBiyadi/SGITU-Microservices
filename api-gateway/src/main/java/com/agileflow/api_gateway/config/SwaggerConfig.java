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
                        .title("SGITU - API Gateway G10")
                        .version("1.0.0")
                        .description("""
                                **API Gateway & Securite** - Groupe 10 SGITU.

                                G10 est le point d'entree unique du systeme SGITU.
                                Il ne possede pas la base officielle des comptes utilisateurs.
                                G3 Gestion des utilisateurs est la source de verite pour :
                                - profils utilisateurs ;
                                - roles et permissions ;
                                - authentification et emission JWT.

                                G10 valide les JWT emis par G3, applique les regles d'acces,
                                propage `X-User-Id`, `X-User-Email`, `X-Roles`,
                                `X-Correlation-Id`, puis route vers G1-G9.
                                """)
                        .contact(new Contact()
                                .name("Groupe 10 - API Gateway & Securite")
                        )
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Developpement local"),
                        new Server().url("http://api-gateway:8080").description("Docker")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token JWT emis par G3")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
