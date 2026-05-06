package ma.sgitu.g5.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public : health check + Swagger/OpenAPI
                .requestMatchers(
                    "/api/notifications/health",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/api-docs",
                    "/v3/api-docs/**",
                    "/webjars/**"
                ).permitAll()
                
                // Tout le reste = JWT obligatoire (à implémenter par l'équipe)
                .anyRequest().permitAll()  // ← Remplace les 2 lignes .requestMatchers + .anyRequest
            );
        
        return http.build();
    }
}