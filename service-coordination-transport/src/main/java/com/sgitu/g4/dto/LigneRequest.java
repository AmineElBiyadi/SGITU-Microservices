package com.sgitu.g4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Création / mise à jour ligne")
public class LigneRequest {

	@NotBlank
	@Size(max = 64)
	private String code;

	@NotBlank
	@Size(max = 200)
	private String nom;

	@Size(max = 2000)
	private String description;

	@NotNull
	private Boolean active;
}
