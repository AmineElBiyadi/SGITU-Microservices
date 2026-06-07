package com.sgitu.apigateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    public Claims validateAndExtractClaims(String token) {
        Claims claims = extractAllClaims(token);

        if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT expire");
        }

        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT sans subject");
        }

        List<String> roles = extractRoles(claims);
        if (roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT sans role");
        }

        return claims;
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractRole(String token) {
        return extractRole(extractAllClaims(token));
    }

    public String extractRole(Claims claims) {
        return extractRoles(claims)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<String> extractRoles(Claims claims) {
        List<String> roles = new ArrayList<>();
        addRoles(roles, claims.get("role"));
        addRoles(roles, claims.get("roles"));

        return roles.stream()
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .distinct()
                .toList();
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

    public String extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId == null) {
            userId = claims.get("id");
        }
        return userId == null ? null : userId.toString();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
