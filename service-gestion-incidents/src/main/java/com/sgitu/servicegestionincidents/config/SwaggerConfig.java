package com.sgitu.servicegestionincidents.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGITU - API Gestion des Incidents")
                        .version("1.0")
                        .description("Microservice de gestion des incidents pour le système SGITU")
                        .contact(new Contact()
                                .name("Groupe 9")
                                .email("groupe9@sgitu.com")));
    }
}
