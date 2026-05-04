package com.ensate.billetterie.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration Spring pour les beans d'infrastructure HTTP et d'exécution asynchrone.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean(name = "validationExecutor")
    public Executor validationExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
