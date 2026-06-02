package com.sgitu.apigateway.filter;

import com.sgitu.apigateway.security.JwtPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GatewayHeadersFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String ROLES_HEADER = "X-Roles";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String SOURCE_GROUP_HEADER = "X-Source-Group";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange);

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(authentication -> enrichExchange(exchange, authentication, correlationId))
                .defaultIfEmpty(enrichExchange(exchange, null, correlationId))
                .flatMap(enrichedExchange -> {
                    enrichedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);
                    logRequest(enrichedExchange, correlationId);
                    return chain.filter(enrichedExchange)
                            .doOnSuccess(ignored -> logResponse(enrichedExchange, correlationId))
                            .doOnError(error -> logGatewayError(enrichedExchange, correlationId, error));
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    private ServerWebExchange enrichExchange(ServerWebExchange exchange,
                                             Authentication authentication,
                                             String correlationId) {
        var requestBuilder = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_EMAIL_HEADER);
                    headers.remove(ROLES_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove(SOURCE_GROUP_HEADER);
                    headers.remove(TRACE_ID_HEADER);
                })
                .header(CORRELATION_ID_HEADER, correlationId);

        if (authentication != null && authentication.isAuthenticated()) {
            String roles = extractRoles(authentication);
            requestBuilder.header(ROLES_HEADER, roles);
            firstRole(roles).ifPresent(role -> requestBuilder.header(USER_ROLE_HEADER, role));
            requestBuilder.header(SOURCE_GROUP_HEADER, "G10");

            if (authentication.getPrincipal() instanceof JwtPrincipal principal) {
                if (principal.email() != null && !principal.email().isBlank()) {
                    requestBuilder.header(USER_EMAIL_HEADER, principal.email());
                }
                if (principal.userId() != null && !principal.userId().isBlank()) {
                    requestBuilder.header(USER_ID_HEADER, principal.userId());
                }
            } else {
                requestBuilder.header(USER_EMAIL_HEADER, authentication.getName());
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

    private java.util.Optional<String> firstRole(String roles) {
        if (roles == null || roles.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .findFirst();
    }

    private void logRequest(ServerWebExchange exchange, String correlationId) {
        log.info(
                "Gateway request correlationId={} method={} path={} user={} roles={}",
                correlationId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getRawPath(),
                exchange.getRequest().getHeaders().getFirst(USER_EMAIL_HEADER),
                exchange.getRequest().getHeaders().getFirst(ROLES_HEADER)
        );
    }

    private void logResponse(ServerWebExchange exchange, String correlationId) {
        var status = exchange.getResponse().getStatusCode();
        log.info(
                "Gateway response correlationId={} status={} path={}",
                correlationId,
                status != null ? status.value() : 200,
                exchange.getRequest().getURI().getRawPath()
        );
    }

    private void logGatewayError(ServerWebExchange exchange, String correlationId, Throwable error) {
        log.warn(
                "Gateway error correlationId={} path={} cause={}",
                correlationId,
                exchange.getRequest().getURI().getRawPath(),
                error.getMessage()
        );
    }
}
