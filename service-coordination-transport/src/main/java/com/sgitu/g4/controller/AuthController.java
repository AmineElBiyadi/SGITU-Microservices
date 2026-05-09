package com.sgitu.g4.controller;

import com.sgitu.g4.config.JwtProperties;
import com.sgitu.g4.dto.LoginRequest;
import com.sgitu.g4.dto.TokenResponse;
import com.sgitu.g4.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentification")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;

	@PostMapping("/login")
	@Operation(summary = "Obtenir un JWT")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
		String token = jwtTokenProvider.createToken(authentication.getName(), authentication.getAuthorities());
		String primaryRole = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
				.findFirst()
				.orElse("G4_OPERATOR");
		return ResponseEntity.ok(TokenResponse.builder()
				.token(token)
				.type("Bearer")
				.expiresIn(jwtProperties.getExpirationMs())
				.role(primaryRole)
				.build());
	}
}
