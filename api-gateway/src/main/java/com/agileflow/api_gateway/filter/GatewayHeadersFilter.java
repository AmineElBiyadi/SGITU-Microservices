package com.agileflow.api_gateway.filter;

import com.agileflow.api_gateway.model.User;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GatewayHeadersFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange);

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(authentication -> enrichExchange(exchange, authentication, correlationId))
                .defaultIfEmpty(enrichExchange(exchange, null, correlationId))
                .flatMap(enrichedExchange -> {
                    enrichedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);
                    return chain.filter(enrichedExchange);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private ServerWebExchange enrichExchange(ServerWebExchange exchange,
                                             Authentication authentication,
                                             String correlationId) {
        var requestBuilder = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID_HEADER, correlationId);

        if (authentication != null && authentication.isAuthenticated()) {
            requestBuilder.header("X-User-Email", authentication.getName());
            requestBuilder.header("X-Roles", extractRoles(authentication));

            if (authentication.getPrincipal() instanceof User user && user.getId() != null) {
                requestBuilder.header("X-User-Id", user.getId().toString());
            }
        }

        return exchange.mutate().request(requestBuilder.build()).build();
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String existingCorrelationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_ID_HEADER);

        if (existingCorrelationId != null && !existingCorrelationId.isBlank()) {
            return existingCorrelationId;
        }

        return UUID.randomUUID().toString();
    }

    private String extractRoles(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }
}
