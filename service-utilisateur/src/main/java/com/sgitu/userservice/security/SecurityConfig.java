package com.sgitu.userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final InternalKeyFilter internalKeyFilter;

    @Value("${internal.key}")
    private String internalKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public — inscription
                .requestMatchers(HttpMethod.POST, "/users").permitAll()

                // Swagger / OpenAPI
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml"
                ).permitAll()

                // Endpoint interne G10 — protégé par X-Internal-Key (InternalKeyFilter)
                .requestMatchers("/users/internal/**").hasRole("INTERNAL")

                // Vérification d'existence — authentifié
                .requestMatchers(HttpMethod.GET, "/users/*/exists").authenticated()

                // Admin endpoints
                .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/users/*/roles").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/users/*/deactivate").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/users/*/activate").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/users/*").hasRole("ADMIN")

                // Tout le reste — authentifié
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(internalKeyFilter, JwtFilter.class);

        return http.build();
    }
}
