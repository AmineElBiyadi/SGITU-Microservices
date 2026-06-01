package ma.sgitu.g5.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds the local Spring Security context from headers already trusted by the
 * API Gateway. JWT signature validation is centralized in the gateway.
 */
@Slf4j
@Component
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USER_EMAIL = "X-User-Email";
    private static final String X_ROLES = "X-Roles";
    private static final String X_SOURCE_GROUP = "X-Source-Group";
    private static final String X_CORRELATION_ID = "X-Correlation-Id";
    private static final String X_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);
        response.setHeader(X_CORRELATION_ID, correlationId);
        response.setHeader(X_TRACE_ID, correlationId);

        String userEmail = request.getHeader(X_USER_EMAIL);
        String userId = request.getHeader(X_USER_ID);
        String rolesHeader = request.getHeader(X_ROLES);
        String sourceGroup = request.getHeader(X_SOURCE_GROUP);

        if (!isBlank(userEmail) || !isBlank(userId)) {
            String principal = !isBlank(userEmail) ? userEmail : userId;
            List<SimpleGrantedAuthority> authorities = extractAuthorities(rolesHeader);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("[GATEWAY-AUTH] CorrelationId={} | User={} | Roles={} | SourceGroup={}",
                    correlationId, principal, rolesHeader, sourceGroup);
        } else {
            log.debug("[GATEWAY-AUTH] CorrelationId={} | No gateway identity headers found", correlationId);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(X_CORRELATION_ID);
        if (!isBlank(correlationId)) {
            return correlationId;
        }

        String traceId = request.getHeader(X_TRACE_ID);
        if (!isBlank(traceId)) {
            return traceId;
        }

        return UUID.randomUUID().toString();
    }

    private List<SimpleGrantedAuthority> extractAuthorities(String rolesHeader) {
        if (isBlank(rolesHeader)) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
