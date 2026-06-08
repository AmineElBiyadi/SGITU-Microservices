package com.sgitu.servicegestionincidents.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * Génère un JWT de service pour G9 au démarrage.
 * Ce token est utilisé uniquement pour les appels inter-service vers G3 (UtilisateurClient).
 * Il est signé avec le même secret que G3 → G3 l'accepte comme valide.
 * Le token a le rôle ROLE_SUPERVISOR, requis par G3 pour GET /users/roles/{role}.
 */
@Component
@Slf4j
public class ServiceJwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Getter
    private String serviceToken;

    @PostConstruct
    public void init() {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        // Validité 1 suffisant pour un service interne
        Date expiry = new Date(now.getTime() + 365L * 24 * 60 * 60 * 1000);

        this.serviceToken = Jwts.builder()
                .setSubject("g9-service@internal.sgitu")
                .claim("userId", 0L)
                .claim("roles", List.of("ROLE_SUPERVISOR"))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.info("Service JWT G9 généré avec succès (valable 1 an)");
    }
}
