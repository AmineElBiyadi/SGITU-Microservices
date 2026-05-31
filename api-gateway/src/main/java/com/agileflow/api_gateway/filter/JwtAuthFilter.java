package com.agileflow.api_gateway.filter;

import com.agileflow.api_gateway.error.ApiErrorWriter;
import com.agileflow.api_gateway.security.JwtPrincipal;
import com.agileflow.api_gateway.service.JwtService;
import com.agileflow.api_gateway.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;
    private final ApiErrorWriter errorWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = extractToken(exchange);

        if (token == null) {
            return chain.filter(exchange);
        }

        return tokenRevocationService.isRevoked(token)
                .flatMap(revoked -> {
                    if (revoked) {
                        return errorWriter.write(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                "TOKEN_REVOKED",
                                "JWT revoque apres logout"
                        );
                    }

                    try {
                        Claims claims = jwtService.validateAndExtractClaims(token);
                        String email = claims.getSubject();
                        String userId = jwtService.extractUserId(claims);
                        List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(claims)
                                .stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList();

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        new JwtPrincipal(userId, email),
                                        token,
                                        authorities
                                );

                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                    } catch (Exception ignored) {
                        return errorWriter.write(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                "INVALID_TOKEN",
                                "JWT invalide ou expire"
                        );
                    }
                });
    }

    public String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}
