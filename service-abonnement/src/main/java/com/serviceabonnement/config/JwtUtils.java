package com.serviceabonnement.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private javax.crypto.SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        String email = (String) claims.get("email");
        if (email == null) {
            email = claims.getSubject();
        }
        return email;
    }

    /**
     * Extracts the list of roles from the JWT token.
     * Supports two claim formats emitted by the User Service (G3):
     *   - "roles" : ["ROLE_PASSENGER", ...] (preferred — list)
     *   - "role"  : "ROLE_STUDENT"          (fallback — single string)
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);

        // Primary: list of roles under "roles" key
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }

        // Fallback: single role string under "role" key
        Object roleObj = claims.get("role");
        if (roleObj instanceof String) {
            return List.of((String) roleObj);
        }

        return List.of();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())   // JJWT 0.12+ API (replaces deprecated setSigningKey)
                .build()
                .parseSignedClaims(token)   // JJWT 0.12+ API (replaces deprecated parseClaimsJws)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token); // throws if signature invalid or token expired
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

