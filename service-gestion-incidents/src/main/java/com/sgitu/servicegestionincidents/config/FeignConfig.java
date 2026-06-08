package com.sgitu.servicegestionincidents.config;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Feign pour les appels inter-service vers G3.
 * Injecte automatiquement le JWT de service G9 dans chaque requête Feign
 * afin de satisfaire la sécurité de G3 (ROLE_SUPERVISOR requis pour /users/roles/*).
 */
@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final ServiceJwtTokenProvider serviceJwtTokenProvider;

    @Bean
    public RequestInterceptor serviceTokenInterceptor() {
        return template -> template.header(
                "Authorization",
                "Bearer " + serviceJwtTokenProvider.getServiceToken()
        );
    }
}
