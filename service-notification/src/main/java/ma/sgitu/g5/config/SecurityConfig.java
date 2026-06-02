package ma.sgitu.g5.config;

import lombok.RequiredArgsConstructor;
import ma.sgitu.g5.security.GatewayHeaderAuthenticationFilter;
import ma.sgitu.g5.security.JWTAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter;

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
                
                // Endpoints d'administration (nécessitent ROLE_ADMIN)
                .requestMatchers("/api/notifications/admin/**").hasRole("ADMIN")
                
                // Tout le reste : exige une identite deja validee et transmise par l'API Gateway
                .anyRequest().authenticated()
            )
            // On ajoute le filtre JWT d'abord
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Puis le filtre Gateway (qui sera ignoré si le JWT a déjà authentifié la requête)
            .addFilterAfter(gatewayHeaderAuthenticationFilter, JWTAuthenticationFilter.class);
        
        return http.build();
    }
}
