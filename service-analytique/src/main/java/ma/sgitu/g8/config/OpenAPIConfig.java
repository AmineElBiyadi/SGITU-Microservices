package ma.sgitu.g8.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .tags(tagList());
    }

    private Info apiInfo() {
        return new Info()
                .title("Data Analysis API")
                .description("API RESTful pour lancer, surveiller et gérer des analyses de données. " +
                          "Ce microservice fournit des endpoints pour effectuer des analyses statistiques, " +
                          "de régression et de classification sur des jeux de données.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("SGITU Team")
                        .email("support@sgitu.ma")
                        .url("https://www.sgitu.ma"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> serverList() {
        Server devServer = new Server()
                .url("http://localhost:8088")
                .description("Serveur de développement");
        
        Server prodServer = new Server()
                .url("https://api.sgitu.ma")
                .description("Serveur de production");
        
        return List.of(devServer, prodServer);
    }

    private List<Tag> tagList() {
        Tag analyticsTag = new Tag()
                .name("Analytics")
                .description("Endpoints pour récupérer les statistiques et rapports analytiques");
        
        Tag analysisTag = new Tag()
                .name("Data Analysis API")
                .description("Endpoints pour lancer, surveiller et gérer les analyses de données");
        
        Tag ingestionTag = new Tag()
                .name("Data Ingestion")
                .description("Endpoints pour l'ingestion des données brutes");
        
        return List.of(analyticsTag, analysisTag, ingestionTag);
    }
}
