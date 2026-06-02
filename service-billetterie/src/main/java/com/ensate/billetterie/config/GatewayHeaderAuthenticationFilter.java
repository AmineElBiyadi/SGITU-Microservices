package com.ensate.billetterie.config;


import com.ensate.billetterie.context.RequestContext;
import com.ensate.billetterie.context.RequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId      = request.getHeader("X-User-Id");
        String userEmail   = request.getHeader("X-User-Email");
        String userRoles   = request.getHeader("X-Roles");
        String correlationId = request.getHeader("X-Correlation-Id");

        try {
            if (userId != null && userEmail != null) {
                List<String> roleList = userRoles != null && !userRoles.isBlank()
                        ? Arrays.stream(userRoles.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        // gateway sends raw authority strings e.g. "ROLE_ADMIN" already
                        .collect(Collectors.toList())
                        : List.of();

                List<SimpleGrantedAuthority> authorities = roleList.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // --- Populate Spring Security context ---
                AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                        UUID.fromString(userId),
                        userEmail,
                        authorities
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);

                SecurityContext secCtx = SecurityContextHolder.createEmptyContext();
                secCtx.setAuthentication(authentication);
                SecurityContextHolder.setContext(secCtx);

                // --- Populate ThreadLocal context ---
                RequestContextHolder.set(
                        RequestContext.builder()
                                .userId(UUID.fromString(userId))
                                .userEmail(userEmail)
                                .roles(roleList)
                                .correlationId(correlationId)
                                .build()
                );

                log.debug("[{}] Authenticated user: {}, roles: {}", correlationId, userEmail, roleList);
            }

            filterChain.doFilter(request, response);

        } finally {
            RequestContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
