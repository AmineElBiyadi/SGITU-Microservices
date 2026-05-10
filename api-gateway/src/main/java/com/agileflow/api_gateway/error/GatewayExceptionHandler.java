package com.agileflow.api_gateway.error;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

@Component
@Order(-2)
@RequiredArgsConstructor
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ApiErrorWriter errorWriter;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);
        return errorWriter.write(
                exchange,
                status,
                resolveCode(status),
                resolveMessage(status, ex)
        );
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof AuthenticationException || hasCause(ex, AuthenticationException.class)) {
            return HttpStatus.UNAUTHORIZED;
        }

        if (ex instanceof ResponseStatusException responseStatusException) {
            HttpStatusCode statusCode = responseStatusException.getStatusCode();
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (hasCause(ex, ConnectException.class)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String resolveCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case CONFLICT -> "CONFLICT";
            case NOT_FOUND -> "ROUTE_NOT_FOUND";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
            default -> "GATEWAY_ERROR";
        };
    }

    private String resolveMessage(HttpStatus status, Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException
                && responseStatusException.getReason() != null
                && !responseStatusException.getReason().isBlank()) {
            return responseStatusException.getReason();
        }

        return switch (status) {
            case BAD_REQUEST -> "Requete invalide";
            case CONFLICT -> "Ressource deja existante";
            case NOT_FOUND -> "Route gateway introuvable";
            case UNAUTHORIZED -> "Authentification requise ou token invalide";
            case FORBIDDEN -> "Acces refuse pour ce role";
            case SERVICE_UNAVAILABLE -> "Microservice cible indisponible";
            default -> "Erreur technique au niveau de la gateway";
        };
    }
}
