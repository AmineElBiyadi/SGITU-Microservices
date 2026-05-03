package com.sgitu.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre pour les communications inter-microservices.
 * Vérifie la présence du header X-Internal-Key pour les endpoints /users/internal/**.
 */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    @Value("${internal.key}")
    private String internalKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // getServletPath() retourne le chemin après le context-path (/api)
        String path = request.getServletPath();
        
        // On n'applique le filtre que sur les endpoints internes
        if (path.startsWith("/users/internal/")) {
            String requestKey = request.getHeader("X-Internal-Key");

            if (internalKey.equals(requestKey)) {
                // Authentification manuelle pour le contexte de sécurité (Rôle interne)
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "INTERNAL_SYSTEM", 
                        null, 
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // Clé absente ou incorrecte
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Accès refusé : Clé interne invalide ou manquante.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
