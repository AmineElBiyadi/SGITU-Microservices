package com.agileflow.api_gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;
import java.util.Date;
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

        String role = extractRole(claims);
        if (role == null || role.isBlank()) {
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
        String role = claims.get("role", String.class);
        if (role == null) {
            role = claims.get("roles", String.class);
        }
        return role;
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
