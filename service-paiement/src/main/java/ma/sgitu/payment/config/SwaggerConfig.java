package ma.sgitu.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/payments",
            "/payments/{paymentId}/refund",
            "/payments/{ticketId}/cancel",
            "/test-cards",
            "/test-mobile-money-accounts",
            "/health"
    );

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

    @Bean
    public OpenApiCustomizer publicEndpointsCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (PUBLIC_PATHS.contains(path)) {
                pathItem.readOperations().forEach(operation -> operation.setSecurity(Collections.emptyList()));
            }
        });
    }
}
