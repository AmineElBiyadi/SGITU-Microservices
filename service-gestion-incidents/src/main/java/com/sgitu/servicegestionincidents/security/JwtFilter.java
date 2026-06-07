package com.sgitu.servicegestionincidents.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtre JWT — Lit et valide le token Bearer.
 * Le service de gestion des incidents ne fait que valider les tokens émis par le service utilisateur.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String email = claims.getSubject();

                List<String> roles = extractRoles(claims);

                if (email != null && roles != null) {
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // Token invalide — on ne met rien dans le SecurityContext
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private List<String> extractRoles(Claims claims) {
        List<String> roles = new ArrayList<>();
        addRoles(roles, claims.get("role"));
        addRoles(roles, claims.get("roles"));
        return roles.stream()
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private void addRoles(List<String> roles, Object claimValue) {
        if (claimValue == null) {
            return;
        }
        if (claimValue instanceof Collection<?> collection) {
            collection.forEach(value -> addRoles(roles, value));
            return;
        }
        if (claimValue instanceof String value) {
            for (String role : value.split(",")) {
                roles.add(role);
            }
            return;
        }
        roles.add(claimValue.toString());
    }
}
