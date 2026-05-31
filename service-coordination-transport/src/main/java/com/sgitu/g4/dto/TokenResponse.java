package com.sgitu.g4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Réponse JWT")
public class TokenResponse {

	@Schema(example = "eyJhbGciOiJIUzM4NCJ9...")
	private String token;

	@Schema(example = "Bearer")
	private String type;

	@Schema(description = "Durée en ms")
	private long expiresIn;

	@Schema(example = "GESTIONNAIRE_RESEAU")
	private String role;
}
