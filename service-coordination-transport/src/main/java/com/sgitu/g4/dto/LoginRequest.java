package com.sgitu.g4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Identifiants pour obtenir un JWT")
public class LoginRequest {

	@NotBlank
	@Size(max = 120)
	@Schema(example = "gestionnaire.reseau")
	private String username;

	@NotBlank
	@Size(max = 200)
	@Schema(example = "password")
	private String password;
}
