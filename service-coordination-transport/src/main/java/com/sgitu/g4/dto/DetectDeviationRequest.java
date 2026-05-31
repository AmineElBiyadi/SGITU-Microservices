package com.sgitu.g4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DetectDeviationRequest {

	@NotNull
	private Long missionId;

	@NotBlank
	@Size(max = 2000)
	private String details;
	private Integer ecartMetres;
}
