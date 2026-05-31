package com.sgitu.g4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Création / mise à jour arrêt")
public class ArretRequest {

	@NotBlank
	@Size(max = 64)
	private String code;

	@NotBlank
	@Size(max = 200)
	private String nom;

	private BigDecimal latitude;
	private BigDecimal longitude;

	@NotNull
	private Long ligneId;
}
