package com.sgitu.g4.security;

import com.sgitu.g4.config.IntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Émission du JWT machine G4 pour les appels REST directs vers G3, G5, G7 (promo §3).
 */
@Component
@RequiredArgsConstructor
public class ServiceJwtProvider {

	private final JwtTokenProvider jwtTokenProvider;
	private final IntegrationProperties integrationProperties;

	public String createOutboundToken() {
		return jwtTokenProvider.createServiceToken(
				integrationProperties.getInterServicePrincipal(),
				parseRoles(integrationProperties.getInterServiceRoles()),
				integrationProperties.getInterServiceSourceGroup());
	}

	/** Token G3 : {@code ROLE_G4_OPERATOR} (+ dispatcher si configuré). */
	public String createG3Token() {
		if (StringUtils.hasText(integrationProperties.getG3ServiceBearerToken())) {
			return integrationProperties.getG3ServiceBearerToken().trim();
		}
		return jwtTokenProvider.createServiceToken(
				integrationProperties.getInterServicePrincipal(),
				parseRoles(integrationProperties.getG3InterServiceRoles()),
				integrationProperties.getInterServiceSourceGroup());
	}

	private static List<String> parseRoles(String csv) {
		if (!StringUtils.hasText(csv)) {
			return List.of("G4_OPERATOR");
		}
		return Arrays.stream(csv.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.toList();
	}
}
