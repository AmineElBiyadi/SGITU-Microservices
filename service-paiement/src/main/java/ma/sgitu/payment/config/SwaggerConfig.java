package ma.sgitu.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Microservice G6 Paiement - SGITU")
                        .version("1.0.0")
                        .description("Documentation complète de l'API du microservice de paiement pour le projet SGITU. Ce microservice gère les transactions de paiement, les remboursements, la génération de factures et l'enregistrement de moyens de paiement (carte et mobile money). Intègre la sécurité JWT, TLS et la communication asynchrone via Kafka.")
                        .contact(new Contact().name("G6 Paiement").email("contact@sgitu.ma")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}