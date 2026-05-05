package ma.sgitu.g5.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("G5 - Notification Service API")
                        .version("1.0.0")
                        .description("Microservice Notifications SGITU - Gestion des emails, SMS et push notifications")
                        .contact(new Contact()
                                .name("Équipe G5 - Notifications")
                                .email("g5-notifications@sgitu.ma"))
                        .license(new License()
                                .name("Propriétaire")
                                .url("https://sgitu.ma")));
    }
}