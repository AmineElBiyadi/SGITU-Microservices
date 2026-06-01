package ma.sgitu.g5.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.service.ITracingService;

/**
 * JWTAuthenticationFilter — Filtre d'authentification JWT multi-groupes.
 *
 * STRATÉGIE D'ACCEPTATION DES TOKENS (G1–G10) :
 * ─────────────────────────────────────────────
 * G5 (Notification) est un service transverse utilisé par tous les groupes.
 * Les tokens sont émis par chaque groupe et signés avec le secret partagé
 * fourni par G10 (Auth). G5 valide la signature avec ce secret commun.
 *
 * TRAÇABILITÉ VERS G10 :
 * ─────────────────────
 * Chaque requête authentifiée est tracée via Kafka (topic: token-validation)
 * vers G10 pour audit asynchrone. G3 (Utilisateurs) bénéficie d'un tag prioritaire.
 *
 * COMPORTEMENT EN CAS D'ERREUR :
 * ──────────────────────────────
 * - Token absent       → 401 Unauthorized (sauf endpoints publics gérés par SecurityConfig)
 * - Token expiré       → 401 Unauthorized avec message explicite
 * - Signature invalide → 401 Unauthorized avec message explicite
 * - Token mal formé    → 401 Unauthorized
 */
@Slf4j
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ITracingService tracingService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";
    private static final String X_SOURCE_GROUP       = "X-Source-Group";
    private static final String X_TRACE_ID           = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader  = request.getHeader(AUTHORIZATION_HEADER);
        String sourceGroup = request.getHeader(X_SOURCE_GROUP);
        String traceId     = request.getHeader(X_TRACE_ID);

        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        // ── 1. Token absent ──────────────────────────────────────────────────
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("[JWT] TraceId={} | Token JWT absent — 401 retourné", traceId);
            sendUnauthorized(response, "Token JWT manquant. Fournissez un header Authorization: Bearer <token>");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        String userId            = null;
        String tokenSourceService = null;
        List<String> roles       = null;
        boolean tokenValid       = false;

        try {
            // ── 2. Validation signature + parsing ────────────────────────────
            // La clé secrète est celle fournie par G10 (shared secret).
            // Tous les groupes (G1-G10) signent leurs tokens avec cette même clé.
                Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            userId             = claims.getSubject(); // Correspond au 'sub' (email)
            tokenSourceService = claims.get("sourceService", String.class);
            
            // Le Gateway envoie 'role' (singulier) : ROLE_USER, ROLE_ADMIN, etc.
            // On le convertit en liste pour la compatibilité interne
            Object roleClaim = claims.get("role");
            if (roleClaim instanceof String) {
                roles = Collections.singletonList((String) roleClaim);
            } else {
                roles = claims.get("roles", List.class);
            }
            tokenValid         = true;

            log.info("[JWT] TraceId={} | Token valide | User={} | Source={} | Groupe={}",
                    traceId, userId, tokenSourceService, sourceGroup);

        } catch (ExpiredJwtException ex) {
            log.warn("[JWT] TraceId={} | Token expiré : {}", traceId, ex.getMessage());
            sendUnauthorized(response, "Token JWT expiré. Veuillez vous ré-authentifier.");
            return;

        } catch (SignatureException ex) {
            log.warn("[JWT] TraceId={} | Signature JWT invalide (token d'un groupe non reconnu) : {}",
                    traceId, ex.getMessage());
            sendUnauthorized(response, "Signature JWT invalide. Token non reconnu par G5.");
            return;

        } catch (MalformedJwtException ex) {
            log.warn("[JWT] TraceId={} | Token JWT mal formé : {}", traceId, ex.getMessage());
            sendUnauthorized(response, "Token JWT mal formé.");
            return;

        } catch (Exception ex) {
            log.warn("[JWT] TraceId={} | Erreur d'authentification : {}", traceId, ex.getMessage());
            sendUnauthorized(response, "Erreur d'authentification JWT.");
            return;
        }

        // ── 3. Traitement spécial G3 (Utilisateurs) ─────────────────────────
        if ("G3".equals(sourceGroup)) {
            log.info("[JWT] TraceId={} | [G3-PRIORITAIRE] Requête du groupe Utilisateurs | User={}",
                    traceId, userId);
        }

        // ── 4. Traçabilité asynchrone vers G10 via Kafka ─────────────────────
        tracingService.sendTracingInfo(traceId, token, sourceGroup,
                tokenSourceService, userId, roles, tokenValid);

        // ── 5. Authentification Spring Security ──────────────────────────────
        List<SimpleGrantedAuthority> authorities = (roles != null && !roles.isEmpty())
                ? roles.stream().map(SimpleGrantedAuthority::new).toList()
                : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ── 6. Propagation du Trace ID en réponse ────────────────────────────
        response.setHeader(X_TRACE_ID, traceId);

        filterChain.doFilter(request, response);
    }

    /**
     * Envoie une réponse HTTP 401 avec un message JSON structuré.
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}"
        );
    }
}
