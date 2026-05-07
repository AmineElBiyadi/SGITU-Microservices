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

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Documentation et Actuator (Public)
                .requestMatchers("/api/auth/test/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/**").permitAll()
                
                // Gestion des véhicules (Admin uniquement)
                .requestMatchers(HttpMethod.POST, "/api/suivi-vehicules/vehicules/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/suivi-vehicules/vehicules/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/suivi-vehicules/vehicules/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN", "ROLE_AGENT")
                
                // Positions (Accessibles par les Agents et Admins)
                .requestMatchers(HttpMethod.POST, "/api/suivi-vehicules/positions/**").hasAnyAuthority("ROLE_AGENT", "ROLE_ADMIN")
                .requestMatchers("/api/suivi-vehicules/positions/**").authenticated()
                
                // Tout le reste authentifié
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
