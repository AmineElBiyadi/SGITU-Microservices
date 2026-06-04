package com.sgitu.g4.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Promo §2 : identité utilisateur propagée par G10 via {@code X-User-*} (pas de re-validation JWT locale).
 */
@Slf4j
@Component
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

	public static final String X_USER_ID = "X-User-Id";
	public static final String X_USER_EMAIL = "X-User-Email";
	public static final String X_ROLES = "X-Roles";
	public static final String X_CORRELATION_ID = "X-Correlation-Id";

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String userEmail = request.getHeader(X_USER_EMAIL);
		String userId = request.getHeader(X_USER_ID);
		String rolesHeader = request.getHeader(X_ROLES);

		if (StringUtils.hasText(userEmail) || StringUtils.hasText(userId)) {
			String principal = StringUtils.hasText(userEmail) ? userEmail.trim() : userId.trim();
			List<SimpleGrantedAuthority> authorities = extractAuthorities(rolesHeader);
			var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
			authentication.setDetails(userId);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			log.debug("Auth gateway X-User-* pour principal={}", principal);
		}

		filterChain.doFilter(request, response);
	}

	private static List<SimpleGrantedAuthority> extractAuthorities(String rolesHeader) {
		if (!StringUtils.hasText(rolesHeader)) {
			return List.of(new SimpleGrantedAuthority("ROLE_USER"));
		}
		return Arrays.stream(rolesHeader.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
				.distinct()
				.map(SimpleGrantedAuthority::new)
				.toList();
	}
}
