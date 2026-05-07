package com.agileflow.api_gateway.controller;

import com.agileflow.api_gateway.error.ApiErrorWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class GatewayFallbackController {

    private final ApiErrorWriter errorWriter;

    @RequestMapping("/api/**")
    public Mono<Void> routeNotFound(ServerWebExchange exchange) {
        return errorWriter.write(
                exchange,
                HttpStatus.NOT_FOUND,
                "ROUTE_NOT_FOUND",
                "Aucune route gateway ne correspond a cette URL"
        );
    }
}
