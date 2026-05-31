package com.sgitu.userservice.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;
/**
 * Service de generation de tokens JWT.
 * G3 est l emetteur officiel des JWT pour tout le systeme SGITU.
 * G10 (API Gateway) se contente de valider la signature.
 *
 * Gère également les refresh tokens (UUID opaques stockes dans Redis).
 */
@Component
@RequiredArgsConstructor
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration:900}")
    private long expirationSeconds;
    @Value("${jwt.refresh-expiration:604800}")
    private long refreshExpirationSeconds;

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    // ── Access Token ──────────────────────────────────────────────

    public String generateToken(Long userId, String email, List<String> roles) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000L);
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Date getTokenExpiration(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public long getTokenTtlSeconds(String token) {
        Date expiration = getTokenExpiration(token);
        if (expiration == null) {
            return 0L;
        }
        long ttl = (expiration.getTime() - new Date().getTime()) / 1000L;
        return Math.max(ttl, 0L);
    }

    // ── Refresh Token (opaque UUID in Redis) ──────────────────────

    /**
     * Génère un refresh token (UUID opaque) et le stocke dans Redis
     * avec les informations utilisateur nécessaires pour émettre un nouveau access token.
     *
     * Format Redis : refresh:<uuid> → "userId|email|role1,role2"
     */
    public String generateRefreshToken(Long userId, String email, List<String> roles) {
        String refreshToken = UUID.randomUUID().toString();
        String value = userId + "|" + email + "|" + String.join(",", roles);
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + refreshToken,
                value,
                Duration.ofSeconds(refreshExpirationSeconds)
        );
        return refreshToken;
    }

    /**
     * Valide un refresh token en vérifiant son existence dans Redis.
     * @return un tableau [userId, email, roles_csv] ou null si invalide/expiré.
     */
    public String[] validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        String value = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + refreshToken);
        if (value == null) {
            return null;
        }
        return value.split("\\|", 3);
    }

    /**
     * Révoque un refresh token (supprime la clé Redis).
     */
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(REFRESH_KEY_PREFIX + refreshToken);
        }
    }
}