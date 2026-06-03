package ma.sgitu.g8.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class HealthCheckConfig {

    @Bean
    public HealthIndicator livenessHealthIndicator() {
        return () -> {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("timestamp", System.currentTimeMillis());
            details.put("type", "liveness");
            details.put("description", "Application is alive and responding");
            
            return Health.up()
                    .withDetails(details)
                    .build();
        };
    }

    @Bean
    public HealthIndicator readinessHealthIndicator(MongoTemplate mongoTemplate) {
        return () -> {
            Map<String, Object> details = new HashMap<>();
            
            try {
                // Test de la connexion MongoDB
                mongoTemplate.executeCommand("{ ping: 1 }");
                details.put("database", "UP");
                details.put("mongodb", "connected");
            } catch (Exception e) {
                details.put("database", "DOWN");
                details.put("mongodb", "disconnected");
                details.put("error", e.getMessage());
                return Health.down()
                        .withDetails(details)
                        .withException(e)
                        .build();
            }
            
            // Test des services externes
            details.put("ml-service", checkExternalService("ML_SERVICE_URL"));
            details.put("notification-service", checkExternalService("G5_NOTIFICATION_URL"));
            
            details.put("status", "UP");
            details.put("timestamp", System.currentTimeMillis());
            details.put("type", "readiness");
            details.put("description", "Application is ready to accept traffic");
            
            return Health.up()
                    .withDetails(details)
                    .build();
        };
    }

    @Bean
    public HealthIndicator dbHealthIndicator(MongoTemplate mongoTemplate) {
        return () -> {
            Map<String, Object> details = new HashMap<>();
            
            try {
                // Test de la connexion MongoDB avec plus de détails
                var commandResult = mongoTemplate.executeCommand("{ serverStatus: 1 }");
                details.put("mongodb", "connected");
                details.put("version", commandResult.getString("version"));
                details.put("uptime", commandResult.get("uptime"));
                details.put("connections", commandResult.get("connections"));
                
                return Health.up()
                        .withDetails(details)
                        .build();
            } catch (Exception e) {
                details.put("mongodb", "disconnected");
                details.put("error", e.getMessage());
                details.put("timestamp", System.currentTimeMillis());
                
                return Health.down()
                        .withDetails(details)
                        .withException(e)
                        .build();
            }
        };
    }

    private Map<String, Object> checkExternalService(String serviceUrlEnv) {
        Map<String, Object> serviceStatus = new HashMap<>();
        
        try {
            String serviceUrl = System.getenv(serviceUrlEnv);
            if (serviceUrl != null && !serviceUrl.isEmpty()) {
                // Simulation de vérification de service externe
                // En production, vous pourriez utiliser WebClient pour un vrai check
                serviceStatus.put("status", "UP");
                serviceStatus.put("url", serviceUrl);
                serviceStatus.put("configured", true);
            } else {
                serviceStatus.put("status", "UNKNOWN");
                serviceStatus.put("url", "not_configured");
                serviceStatus.put("configured", false);
            }
        } catch (Exception e) {
            serviceStatus.put("status", "DOWN");
            serviceStatus.put("error", e.getMessage());
            serviceStatus.put("configured", false);
        }
        
        return serviceStatus;
    }
}
