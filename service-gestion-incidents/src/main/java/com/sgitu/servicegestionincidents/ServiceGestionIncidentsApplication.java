package com.sgitu.servicegestionincidents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.sgitu.servicegestionincidents.client")
@EnableJpaAuditing
public class ServiceGestionIncidentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceGestionIncidentsApplication.class, args);
        System.out.println("✅ Service Gestion Incidents démarré sur http://localhost:8089");
        System.out.println("📚 Swagger UI: http://localhost:8089/api/incidents/swagger-ui.html");
    }

}
