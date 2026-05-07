package com.agileflow.api_gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final ObjectMapper objectMapper;

    public Mono<Void> write(ServerWebExchange exchange,
                            HttpStatus status,
                            String code,
                            String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        String correlationId = resolveCorrelationId(exchange);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());
        body.put("correlationId", correlationId);

        byte[] bytes = toJsonBytes(body);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
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

    private byte[] toJsonBytes(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            String fallback = "{\"status\":500,\"code\":\"JSON_ERROR\",\"message\":\"Cannot serialize error response\"}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
