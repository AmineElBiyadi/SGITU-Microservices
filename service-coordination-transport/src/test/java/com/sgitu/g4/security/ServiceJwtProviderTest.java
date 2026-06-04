package com.sgitu.g4.security;

import com.sgitu.g4.config.IntegrationProperties;
import com.sgitu.g4.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServiceJwtProviderTest {

	private ServiceJwtProvider serviceJwtProvider;
	private JwtProperties jwtProperties;

	@BeforeEach
	void setUp() {
		jwtProperties = new JwtProperties();
		jwtProperties.setSecret("TestSecretKeyForG4UnitTests__128Bits!");
		jwtProperties.setExpirationMs(3_600_000L);
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties);
		IntegrationProperties integrationProperties = new IntegrationProperties();
		integrationProperties.setInterServicePrincipal("g4-coordination-service");
		integrationProperties.setInterServiceSourceGroup("G4");
		integrationProperties.setG3InterServiceRoles("G4_OPERATOR,DISPATCHER");
		serviceJwtProvider = new ServiceJwtProvider(jwtTokenProvider, integrationProperties);
	}

	@Test
	void createOutboundToken_contientSourceServiceEtRoles() throws Exception {
		String token = serviceJwtProvider.createOutboundToken();
		Claims claims = parse(token);
		assertEquals("g4-coordination-service", claims.getSubject());
		assertEquals("G4", claims.get("sourceService", String.class));
		@SuppressWarnings("unchecked")
		List<String> roles = claims.get("roles", List.class);
		assertNotNull(roles);
		assertEquals(List.of("G4_OPERATOR", "SERVICE"), roles);
	}

	@Test
	void createG3Token_contientRolesG3() throws Exception {
		String token = serviceJwtProvider.createG3Token();
		Claims claims = parse(token);
		@SuppressWarnings("unchecked")
		List<String> roles = claims.get("roles", List.class);
		assertEquals(List.of("G4_OPERATOR", "DISPATCHER"), roles);
	}

	private Claims parse(String token) throws Exception {
		byte[] key = MessageDigest.getInstance("SHA-256")
				.digest(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
		return Jwts.parser()
				.verifyWith(Keys.hmacShaKeyFor(key))
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
