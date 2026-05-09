package com.g7suivivehicules.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthFilter headerAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Documentation et Actuator (Public)
                .requestMatchers("/api/auth/test/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/**").permitAll()
                
                // Gestion des véhicules (Admin G7 uniquement)
                .requestMatchers(HttpMethod.POST, "/api/suivi-vehicules/vehicules/**").hasAuthority("ROLE_ADMIN_G7")
                .requestMatchers(HttpMethod.DELETE, "/api/suivi-vehicules/vehicules/**").hasAuthority("ROLE_ADMIN_G7")
                
                // Envoi de positions et alertes (Driver et Admin G7)
                .requestMatchers(HttpMethod.POST, "/api/suivi-vehicules/positions/**").hasAnyAuthority("ROLE_DRIVER", "ROLE_ADMIN_G7")
                .requestMatchers(HttpMethod.POST, "/api/suivi-vehicules/alerts/**").hasAnyAuthority("ROLE_DRIVER", "ROLE_ADMIN_G7")
                
                // Modification des alertes (Annulation/Validation par l'opérateur)
                .requestMatchers(HttpMethod.PUT, "/api/suivi-vehicules/alerts/**").hasAnyAuthority("ROLE_OPERATOR", "ROLE_ADMIN_G7")
                .requestMatchers(HttpMethod.PATCH, "/api/suivi-vehicules/alerts/**").hasAnyAuthority("ROLE_OPERATOR", "ROLE_ADMIN_G7")
                
                // Consultation (Admin G7 et Operateur)
                .requestMatchers(HttpMethod.GET, "/api/suivi-vehicules/**").hasAnyAuthority("ROLE_ADMIN_G7", "ROLE_OPERATOR")
                
                // Tout le reste authentifié
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
