package com.agileflow.api_gateway.config;

import com.agileflow.api_gateway.error.ApiErrorWriter;
import com.agileflow.api_gateway.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ApiErrorWriter errorWriter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> errorWriter.write(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                "UNAUTHORIZED",
                                "Authentification requise ou token invalide"
                        ))
                        .accessDeniedHandler((exchange, ex) -> errorWriter.write(
                                exchange,
                                HttpStatus.FORBIDDEN,
                                "FORBIDDEN",
                                "Acces refuse pour ce role"
                        ))
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/verify-email",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/g4/health", "/api/g4/logs").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/notifications/health").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/plans", "/api/plans/*").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/abonnements/**").permitAll()
                        .pathMatchers(
                                "/api/abonnements/paiement/confirmation",
                                "/api/abonnements/remboursement/confirmation",
                                "/api/abonnements/users/*/actif"
                        ).permitAll()
                        .pathMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/users").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/users/roles/*")
                                .hasAnyAuthority("ROLE_SUPERVISOR", "ROLE_DISPATCHER")
                        .pathMatchers(HttpMethod.GET, "/api/users/notification-recipients")
                                .hasAnyAuthority("ROLE_G4_OPERATOR", "ROLE_DISPATCHER")
                        .pathMatchers(
                                "/api/plans",
                                "/api/plans/**",
                                "/api/abonnements/admin",
                                "/api/abonnements/admin/**"
                        ).hasAuthority("ROLE_ADMIN_G2")
                        .pathMatchers(
                                "/api/users/*/roles",
                                "/api/users/*/activate",
                                "/api/users/*/deactivate",
                                "/api/v1/admin/**",
                                "/api/v1/ticket-types",
                                "/api/v1/ticket-types/**"
                        ).hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/users/*").hasAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/v1/tickets", "/api/v1/tickets/**").authenticated()
                        .pathMatchers(
                                "/api/v1/analytics/**",
                                "/predict/**"
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_OPERATOR", "ROLE_STAFF")
                        .pathMatchers("/api/notifications/admin/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/v1/operator/status")
                                .hasAuthority("ROLE_G4_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/g4/**")
                                .hasAnyAuthority("ROLE_G4_OPERATOR", "ROLE_DISPATCHER", "ROLE_G4_ADMIN")
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/g4/lignes",
                                "/api/g4/lignes/**",
                                "/api/g4/trajets",
                                "/api/g4/trajets/**",
                                "/api/g4/arrets",
                                "/api/g4/arrets/**",
                                "/api/g4/horaires",
                                "/api/g4/horaires/**"
                        ).hasAnyAuthority("ROLE_G4_OPERATOR", "ROLE_G4_ADMIN")
                        .pathMatchers(
                                HttpMethod.PUT,
                                "/api/g4/lignes",
                                "/api/g4/lignes/**",
                                "/api/g4/trajets",
                                "/api/g4/trajets/**",
                                "/api/g4/arrets",
                                "/api/g4/arrets/**",
                                "/api/g4/horaires",
                                "/api/g4/horaires/**"
                        ).hasAnyAuthority("ROLE_G4_OPERATOR", "ROLE_G4_ADMIN")
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/g4/lignes",
                                "/api/g4/lignes/**",
                                "/api/g4/trajets",
                                "/api/g4/trajets/**",
                                "/api/g4/arrets",
                                "/api/g4/arrets/**",
                                "/api/g4/horaires",
                                "/api/g4/horaires/**"
                        ).hasAnyAuthority("ROLE_G4_OPERATOR", "ROLE_G4_ADMIN")
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/g4/missions",
                                "/api/g4/missions/**",
                                "/api/g4/affectations",
                                "/api/g4/affectations/**",
                                "/api/g4/events",
                                "/api/g4/events/**",
                                "/api/g4/incident-impacts",
                                "/api/g4/incident-impacts/**"
                        ).hasAnyAuthority("ROLE_DISPATCHER", "ROLE_G4_ADMIN")
                        .pathMatchers(
                                HttpMethod.PUT,
                                "/api/g4/missions",
                                "/api/g4/missions/**",
                                "/api/g4/affectations",
                                "/api/g4/affectations/**",
                                "/api/g4/events",
                                "/api/g4/events/**"
                        ).hasAnyAuthority("ROLE_DISPATCHER", "ROLE_G4_ADMIN")
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/g4/missions",
                                "/api/g4/missions/**",
                                "/api/g4/affectations",
                                "/api/g4/affectations/**",
                                "/api/g4/events",
                                "/api/g4/events/**"
                        ).hasAnyAuthority("ROLE_DISPATCHER", "ROLE_G4_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/g4/pending-notifications")
                                .hasAuthority("ROLE_G4_ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/g4/pending-notifications", "/api/g4/pending-notifications/**")
                                .hasAuthority("ROLE_G4_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/suivi-vehicules/**")
                                .hasAnyAuthority("ROLE_ADMIN_G7", "ROLE_OPERATOR", "ROLE_TECHNICIAN")
                        .pathMatchers(HttpMethod.POST, "/api/suivi-vehicules/vehicules", "/api/suivi-vehicules/vehicules/**")
                                .hasAuthority("ROLE_ADMIN_G7")
                        .pathMatchers(HttpMethod.PUT, "/api/suivi-vehicules/vehicules", "/api/suivi-vehicules/vehicules/**")
                                .hasAuthority("ROLE_ADMIN_G7")
                        .pathMatchers(HttpMethod.DELETE, "/api/suivi-vehicules/vehicules", "/api/suivi-vehicules/vehicules/**")
                                .hasAuthority("ROLE_ADMIN_G7")
                        .pathMatchers(HttpMethod.POST, "/api/suivi-vehicules/positions", "/api/suivi-vehicules/positions/**")
                                .hasAnyAuthority("ROLE_DRIVER", "ROLE_ADMIN_G7")
                        .pathMatchers(HttpMethod.DELETE, "/api/suivi-vehicules/positions/**")
                                .hasAuthority("ROLE_ADMIN_G7")
                        .pathMatchers(HttpMethod.POST, "/api/suivi-vehicules/alerts", "/api/suivi-vehicules/alerts/**")
                                .hasAnyAuthority("ROLE_DRIVER", "ROLE_ADMIN_G7")
                        .pathMatchers(HttpMethod.PUT, "/api/suivi-vehicules/alerts", "/api/suivi-vehicules/alerts/**")
                                .hasAnyAuthority("ROLE_OPERATOR", "ROLE_ADMIN_G7")
                        .pathMatchers(HttpMethod.PATCH, "/api/suivi-vehicules/alerts", "/api/suivi-vehicules/alerts/**")
                                .hasAnyAuthority("ROLE_OPERATOR", "ROLE_ADMIN_G7")
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
