package com.sgitu.g4.security;

import com.sgitu.g4.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private final JwtProperties jwtProperties;

	public String createToken(String username, Collection<? extends GrantedAuthority> authorities) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + jwtProperties.getExpirationMs());
		List<String> roles = authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
				.collect(Collectors.toList());
		return Jwts.builder()
				.subject(username)
				.claim("roles", roles)
				.issuedAt(now)
				.expiration(exp)
				.signWith(signingKey())
				.compact();
	}

	public Authentication parseToken(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(signingKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
		String username = claims.getSubject();
		@SuppressWarnings("unchecked")
		List<String> roles = claims.get("roles", List.class);
		if (roles == null) {
			roles = List.of();
		}
		Collection<GrantedAuthority> authorities = roles.stream()
				.map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toSet());
		User principal = new User(username, "", authorities);
		return new UsernamePasswordAuthenticationToken(principal, token, authorities);
	}

	private SecretKey signingKey() {
		String secret = jwtProperties.getSecret().trim();
		try {
			byte[] decoded = Base64.getDecoder().decode(secret);
			if (decoded.length >= 32) {
				return Keys.hmacShaKeyFor(decoded);
			}
		} catch (IllegalArgumentException ignored) {
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return Keys.hmacShaKeyFor(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException("Clé JWT invalide", e);
		}
	}
}
